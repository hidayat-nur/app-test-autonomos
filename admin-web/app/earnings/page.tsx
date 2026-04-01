'use client';

import { useState, useEffect, useCallback } from 'react';
import { getMasterApps, getOperationalCosts, addOperationalCost, deleteOperationalCost, type MasterApp, type OperationalCost } from '@/lib/firestore';

type QuickFilter = 'TODAY' | 'THIS_MONTH' | 'THIS_YEAR' | 'ALL_TIME' | 'CUSTOM';

const MONTHS = [
    'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
    'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember',
];

function toYYYYMM(year: number, month: number): string {
    return `${year}-${String(month + 1).padStart(2, '0')}`;
}

function formatRp(amount: number): string {
    return `Rp ${amount.toLocaleString('id-ID')}`;
}

function generateInvoiceNumber(name: string, period: string): string {
    const initials = name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 3);
    const periodClean = period.replace('-', '');
    return `INV-${periodClean}-${initials}`;
}

interface InvoiceData {
    recipientName: string;
    recipientNickname: string;
    period: string;
    periodLabel: string;
    gross: number;
    totalOps: number;
    net: number;
    share: number;
    opsList: OperationalCost[];
}

async function generateInvoicePDF(data: InvoiceData) {
    const { jsPDF } = await import('jspdf');
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

    const pageW = doc.internal.pageSize.getWidth();
    const pageH = doc.internal.pageSize.getHeight();
    const margin = 20;
    const contentW = pageW - margin * 2;

    // ── Background ──────────────────────────────────────────
    doc.setFillColor(15, 23, 42); // slate-900
    doc.rect(0, 0, pageW, 60, 'F');

    // Header accent line
    doc.setFillColor(34, 197, 94); // green-500
    doc.rect(0, 60, pageW, 3, 'F');

    // ── Company Name ────────────────────────────────────────
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(22);
    doc.setTextColor(255, 255, 255);
    doc.text('BORDER TECH', margin, 28);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(148, 163, 184); // slate-400
    doc.text('Sistem Manajemen Aplikasi & Penghasilan', margin, 36);
    doc.text('border-tech.id', margin, 42);

    // ── INVOICE label (top-right) ────────────────────────────
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(28);
    doc.setTextColor(34, 197, 94); // green-500
    doc.text('INVOICE', pageW - margin, 28, { align: 'right' });

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(148, 163, 184);
    const invoiceNo = generateInvoiceNumber(data.recipientName, data.period);
    doc.text(`No: ${invoiceNo}`, pageW - margin, 38, { align: 'right' });

    const today = new Date();
    const dateStr = today.toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' });
    doc.text(`Tanggal: ${dateStr}`, pageW - margin, 45, { align: 'right' });

    // ── Info Block ───────────────────────────────────────────
    let y = 80;

    // Bill To box
    doc.setFillColor(241, 245, 249); // slate-100
    doc.roundedRect(margin, y, (contentW / 2) - 5, 38, 3, 3, 'F');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139); // slate-500
    doc.text('DIPERUNTUKKAN KEPADA', margin + 6, y + 8);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(15, 23, 42);
    doc.text(data.recipientName, margin + 6, y + 18);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(71, 85, 105);
    doc.text(`(${data.recipientNickname})`, margin + 6, y + 26);
    doc.text('Staff / Karyawan', margin + 6, y + 33);

    // Period box
    const boxX2 = margin + (contentW / 2) + 5;
    const boxW2 = (contentW / 2) - 5;
    doc.setFillColor(240, 253, 244); // green-50
    doc.roundedRect(boxX2, y, boxW2, 38, 3, 3, 'F');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    doc.text('PERIODE PEMBAYARAN', boxX2 + 6, y + 8);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(15, 23, 42);
    doc.text(data.periodLabel, boxX2 + 6, y + 18);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(71, 85, 105);
    doc.text('Bagi Hasil Bulanan', boxX2 + 6, y + 26);
    doc.text(`1/3 dari Net Penghasilan`, boxX2 + 6, y + 33);

    // ── Rincian Tabel ────────────────────────────────────────
    y += 50;

    // Table header
    doc.setFillColor(15, 23, 42);
    doc.roundedRect(margin, y, contentW, 10, 2, 2, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(255, 255, 255);
    doc.text('KETERANGAN', margin + 6, y + 6.5);
    doc.text('JUMLAH', pageW - margin - 6, y + 6.5, { align: 'right' });

    y += 10;

    // Row helper
    const drawRow = (label: string, value: string, bg: [number, number, number], textColor: [number, number, number] = [15, 23, 42]) => {
        doc.setFillColor(...bg);
        doc.rect(margin, y, contentW, 10, 'F');
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(9.5);
        doc.setTextColor(...textColor);
        doc.text(label, margin + 6, y + 6.5);
        doc.setFont('helvetica', 'bold');
        doc.text(value, pageW - margin - 6, y + 6.5, { align: 'right' });
        y += 10;
    };

    // Gross
    drawRow('Total Pendapatan Kotor (Gross)', formatRp(data.gross), [248, 250, 252]);

    // Operational costs detail
    if (data.opsList.length > 0) {
        data.opsList.forEach((op, idx) => {
            drawRow(
                `  - ${op.name}`,
                `- ${formatRp(op.amount)}`,
                idx % 2 === 0 ? [255, 247, 237] : [255, 251, 245],
                [194, 65, 12]
            );
        });
    } else {
        drawRow('  - Biaya Operasional', `- ${formatRp(data.totalOps)}`, [255, 247, 237], [194, 65, 12]);
    }

    // Total ops
    doc.setFillColor(254, 226, 226); // red-100
    doc.rect(margin, y, contentW, 10, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9.5);
    doc.setTextColor(185, 28, 28);
    doc.text('Total Potongan Operasional', margin + 6, y + 6.5);
    doc.text(`- ${formatRp(data.totalOps)}`, pageW - margin - 6, y + 6.5, { align: 'right' });
    y += 10;

    // Net
    doc.setFillColor(220, 252, 231); // green-100
    doc.rect(margin, y, contentW, 10, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9.5);
    doc.setTextColor(22, 101, 52);
    doc.text('Net Penghasilan (setelah operasional)', margin + 6, y + 6.5);
    doc.text(formatRp(data.net), pageW - margin - 6, y + 6.5, { align: 'right' });
    y += 10;

    // Divider
    doc.setFillColor(34, 197, 94);
    doc.rect(margin, y, contentW, 1, 'F');
    y += 6;

    // Share (1/3)
    doc.setFillColor(21, 128, 61); // green-700
    doc.roundedRect(margin, y, contentW, 16, 3, 3, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(255, 255, 255);
    doc.text(`Bagian ${data.recipientNickname} (1/3 Net)`, margin + 6, y + 10);
    doc.setFontSize(12);
    doc.text(formatRp(data.share), pageW - margin - 6, y + 10, { align: 'right' });
    y += 16;

    // ── Total Box ────────────────────────────────────────────
    y += 6;
    doc.setFillColor(15, 23, 42);
    doc.roundedRect(margin, y, contentW, 22, 4, 4, 'F');

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.setTextColor(148, 163, 184);
    doc.text('TOTAL YANG DITERIMA', pageW - margin - 6, y + 9, { align: 'right' });

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    doc.setTextColor(34, 197, 94);
    doc.text(formatRp(data.share), pageW - margin - 6, y + 19, { align: 'right' });

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(255, 255, 255);
    doc.text(data.recipientName.toUpperCase(), margin + 6, y + 14);

    y += 22;

    // ── Signature Area ───────────────────────────────────────
    y += 14;
    const sigColW = contentW / 3;

    const drawSigBox = (label: string, name: string, xOff: number) => {
        const bx = margin + xOff;
        doc.setFillColor(248, 250, 252);
        doc.roundedRect(bx, y, sigColW - 4, 38, 3, 3, 'F');
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(8);
        doc.setTextColor(100, 116, 139);
        doc.text(label, bx + (sigColW - 4) / 2, y + 7, { align: 'center' });
        // Signature line
        doc.setDrawColor(203, 213, 225);
        doc.setLineWidth(0.3);
        doc.line(bx + 8, y + 28, bx + sigColW - 12, y + 28);
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(8);
        doc.setTextColor(15, 23, 42);
        doc.text(name, bx + (sigColW - 4) / 2, y + 35, { align: 'center' });
    };

    drawSigBox('Dibuat oleh,', 'Bos Nur', 0);
    drawSigBox('Disetujui oleh,', 'Bos Nur', sigColW);
    drawSigBox('Diterima oleh,', data.recipientNickname, sigColW * 2);

    // ── Footer ───────────────────────────────────────────────
    doc.setFillColor(15, 23, 42);
    doc.rect(0, pageH - 18, pageW, 18, 'F');
    doc.setFillColor(34, 197, 94);
    doc.rect(0, pageH - 18, pageW, 1.5, 'F');

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.setTextColor(148, 163, 184);
    doc.text(
        `${invoiceNo}  |  Digenerate otomatis pada ${dateStr}  |  Border Tech`,
        pageW / 2,
        pageH - 7,
        { align: 'center' }
    );

    // ── Save ─────────────────────────────────────────────────
    const filename = `${invoiceNo}.pdf`;
    doc.save(filename);
}

export default function EarningsDashboard() {
    const [apps, setApps] = useState<MasterApp[]>([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState<QuickFilter>('THIS_MONTH');
    const [generatingPdf, setGeneratingPdf] = useState<string | null>(null);

    const now = new Date();
    const [pickerMonth, setPickerMonth] = useState(now.getMonth());
    const [pickerYear, setPickerYear] = useState(now.getFullYear());

    // Operational costs
    const [ops, setOps] = useState<OperationalCost[]>([]);
    const [opsLoading, setOpsLoading] = useState(false);
    const [newOpName, setNewOpName] = useState('');
    const [newOpAmount, setNewOpAmount] = useState('');
    const [addingOp, setAddingOp] = useState(false);

    // Derive the "active month" for ops based on filter
    const activeMonthYYYYMM: string | null = (() => {
        if (filter === 'CUSTOM') return toYYYYMM(pickerYear, pickerMonth);
        if (filter === 'THIS_MONTH') return toYYYYMM(now.getFullYear(), now.getMonth());
        return null;
    })();

    useEffect(() => {
        const loadApps = async () => {
            setLoading(true);
            try {
                const data = await getMasterApps();
                setApps(data);
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        loadApps();
    }, []);

    const loadOps = useCallback(async () => {
        if (!activeMonthYYYYMM) { setOps([]); return; }
        setOpsLoading(true);
        try {
            const data = await getOperationalCosts(activeMonthYYYYMM);
            setOps(data);
        } catch (e) { console.error(e); }
        finally { setOpsLoading(false); }
    }, [activeMonthYYYYMM]);

    useEffect(() => { loadOps(); }, [loadOps]);

    const handleAddOp = async () => {
        const amount = parseInt(newOpAmount.replace(/\D/g, ''), 10);
        if (!newOpName.trim() || !amount) return;
        if (!activeMonthYYYYMM) return;
        setAddingOp(true);
        try {
            await addOperationalCost({ name: newOpName.trim(), amount, month: activeMonthYYYYMM, createdAt: Date.now() });
            setNewOpName('');
            setNewOpAmount('');
            await loadOps();
        } catch (e) { console.error(e); }
        finally { setAddingOp(false); }
    };

    const handleDeleteOp = async (id: string) => {
        await deleteOperationalCost(id);
        await loadOps();
    };

    const getFilteredApps = (): MasterApp[] => {
        const n = new Date();
        if (filter === 'TODAY') {
            const start = new Date(n.getFullYear(), n.getMonth(), n.getDate()).getTime();
            return apps.filter(a => a.createdAt >= start);
        }
        if (filter === 'THIS_MONTH') {
            const start = new Date(n.getFullYear(), n.getMonth(), 1).getTime();
            return apps.filter(a => a.createdAt >= start);
        }
        if (filter === 'THIS_YEAR') {
            const start = new Date(n.getFullYear(), 0, 1).getTime();
            return apps.filter(a => a.createdAt >= start);
        }
        if (filter === 'CUSTOM') {
            const start = new Date(pickerYear, pickerMonth, 1).getTime();
            const end = new Date(pickerYear, pickerMonth + 1, 1).getTime();
            return apps.filter(a => a.createdAt >= start && a.createdAt < end);
        }
        return apps;
    };

    const filtered = getFilteredApps();
    const gross = filtered.reduce((sum, a) => sum + (a.earning || 0), 0);
    const totalOps = ops.reduce((sum, o) => sum + o.amount, 0);
    const net = Math.max(0, gross - totalOps);
    const count = filtered.length;

    const yearOptions: number[] = [];
    for (let y = 2024; y <= now.getFullYear(); y++) yearOptions.push(y);

    const filterLabel = filter === 'CUSTOM'
        ? `${MONTHS[pickerMonth]} ${pickerYear}`
        : filter === 'THIS_MONTH'
            ? `${MONTHS[now.getMonth()]} ${now.getFullYear()}`
            : filter.replace(/_/g, ' ');

    const activePeriod = activeMonthYYYYMM ?? toYYYYMM(now.getFullYear(), now.getMonth());

    const handleGenerateInvoice = async (
        recipientName: string,
        recipientNickname: string,
    ) => {
        setGeneratingPdf(recipientNickname);
        try {
            await generateInvoicePDF({
                recipientName,
                recipientNickname,
                period: activePeriod,
                periodLabel: filterLabel,
                gross,
                totalOps,
                net,
                share: Math.floor(net / 3),
                opsList: ops,
            });
        } finally {
            setGeneratingPdf(null);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-4xl mx-auto space-y-8">

                <div className="text-center">
                    <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white sm:text-4xl">Earnings Dashboard</h1>
                    <p className="mt-3 text-xl text-gray-500 sm:mt-4">
                        Aggregate financial overview across all registered master clients.
                    </p>
                </div>

                {/* Quick filters */}
                <div className="flex justify-center flex-wrap gap-2">
                    {(['TODAY', 'THIS_MONTH', 'THIS_YEAR', 'ALL_TIME'] as const).map((f) => (
                        <button key={f} onClick={() => setFilter(f)}
                            className={`px-6 py-2 rounded-full text-sm font-bold transition-shadow ${filter === f
                                ? 'bg-green-600 text-white shadow-lg ring-2 ring-green-600 ring-offset-2 dark:ring-offset-gray-900'
                                : 'bg-white text-gray-600 border dark:bg-gray-800 dark:text-gray-300 dark:border-gray-700 hover:bg-gray-50'}`}>
                            {f.replace(/_/g, ' ')}
                        </button>
                    ))}
                </div>

                {/* Month / Year picker */}
                <div className={`flex justify-center items-center gap-2 p-3 rounded-2xl border-2 transition ${filter === 'CUSTOM' ? 'border-green-500 bg-green-50 dark:bg-green-900/20' : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800'}`}>
                    <span className="text-sm font-semibold text-gray-500 dark:text-gray-400 mr-1">📅 Pilih Bulan:</span>
                    <select value={pickerMonth} onChange={e => { setPickerMonth(Number(e.target.value)); setFilter('CUSTOM'); }}
                        className="px-3 py-1.5 rounded-lg border text-sm font-medium bg-white dark:bg-gray-700 dark:text-white dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-green-500">
                        {MONTHS.map((m, i) => <option key={i} value={i}>{m}</option>)}
                    </select>
                    <select value={pickerYear} onChange={e => { setPickerYear(Number(e.target.value)); setFilter('CUSTOM'); }}
                        className="px-3 py-1.5 rounded-lg border text-sm font-medium bg-white dark:bg-gray-700 dark:text-white dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-green-500">
                        {yearOptions.map(y => <option key={y} value={y}>{y}</option>)}
                    </select>
                    {filter !== 'CUSTOM' && (
                        <button onClick={() => setFilter('CUSTOM')}
                            className="px-4 py-1.5 rounded-lg bg-green-600 text-white text-sm font-bold hover:bg-green-700 transition">
                            Lihat
                        </button>
                    )}
                </div>

                {/* Main Metric */}
                <div className="bg-gradient-to-br from-green-500 to-emerald-700 rounded-3xl shadow-xl overflow-hidden relative">
                    <div className="absolute top-0 right-0 -mt-4 -mr-4 w-32 h-32 bg-white opacity-10 rounded-full blur-2xl"></div>
                    <div className="absolute bottom-0 left-0 -mb-4 -ml-4 w-40 h-40 bg-black opacity-10 rounded-full blur-2xl"></div>
                    <div className="px-6 py-16 sm:p-20 text-center relative z-10">
                        <p className="text-green-100 font-semibold uppercase tracking-wider mb-1">
                            {totalOps > 0 ? 'Net Earning (setelah operasional)' : 'Total Project Value'}
                        </p>
                        <p className="text-green-200/70 text-sm mb-4">{filterLabel}</p>
                        {loading ? (
                            <div className="h-20 w-64 bg-green-400/30 animate-pulse rounded-lg mx-auto"></div>
                        ) : (
                            <>
                                <div className="text-5xl sm:text-7xl font-extrabold text-white tracking-tight break-all">
                                    Rp {net.toLocaleString('id-ID')}
                                </div>
                                {totalOps > 0 && (
                                    <p className="mt-2 text-green-200/70 text-sm">
                                        Gross: Rp {gross.toLocaleString('id-ID')} − Ops: Rp {totalOps.toLocaleString('id-ID')}
                                    </p>
                                )}
                            </>
                        )}
                        <p className="mt-4 text-green-100/80 font-medium">From {count} registered client apps</p>
                    </div>
                </div>

                {/* Operational Costs — only for month-scope filters */}
                {activeMonthYYYYMM && (
                    <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6">
                        <h2 className="text-base font-bold text-gray-800 dark:text-white mb-4">🧾 Biaya Operasional — {filterLabel}</h2>

                        {/* Add new */}
                        <div className="flex gap-2 mb-4">
                            <input
                                value={newOpName}
                                onChange={e => setNewOpName(e.target.value)}
                                placeholder="Nama biaya (e.g. Server, Domain)"
                                className="flex-1 border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-400"
                            />
                            <input
                                value={newOpAmount}
                                onChange={e => setNewOpAmount(e.target.value)}
                                placeholder="Nominal (e.g. 150000)"
                                type="number"
                                min={0}
                                className="w-40 border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-400"
                            />
                            <button
                                onClick={handleAddOp}
                                disabled={addingOp || !newOpName.trim() || !newOpAmount}
                                className="px-4 py-2 bg-orange-500 text-white rounded-lg text-sm font-bold hover:bg-orange-600 transition disabled:opacity-50"
                            >
                                + Tambah
                            </button>
                        </div>

                        {/* List */}
                        {opsLoading ? (
                            <p className="text-sm text-gray-400 animate-pulse">Memuat...</p>
                        ) : ops.length === 0 ? (
                            <p className="text-sm text-gray-400 italic">Belum ada biaya operasional untuk bulan ini.</p>
                        ) : (
                            <ul className="space-y-2">
                                {ops.map(op => (
                                    <li key={op.id} className="flex items-center justify-between bg-orange-50 dark:bg-orange-900/20 border border-orange-100 dark:border-orange-800 rounded-lg px-4 py-2">
                                        <span className="text-sm font-medium text-gray-800 dark:text-gray-100">{op.name}</span>
                                        <div className="flex items-center gap-3">
                                            <span className="text-sm font-bold text-orange-600 dark:text-orange-400">
                                                − Rp {op.amount.toLocaleString('id-ID')}
                                            </span>
                                            <button
                                                onClick={() => handleDeleteOp(op.id!)}
                                                className="text-red-400 hover:text-red-600 text-xs font-bold transition"
                                            >
                                                ✕
                                            </button>
                                        </div>
                                    </li>
                                ))}
                                <li className="flex justify-between px-4 py-2 bg-gray-100 dark:bg-gray-700 rounded-lg mt-1">
                                    <span className="text-sm font-bold text-gray-600 dark:text-gray-300">Total Operasional</span>
                                    <span className="text-sm font-bold text-red-600">− Rp {totalOps.toLocaleString('id-ID')}</span>
                                </li>
                            </ul>
                        )}
                    </div>
                )}

                {/* Earnings Split */}
                {!loading && net > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        {/* Bos Nur */}
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-blue-500">
                            <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Bos Nur (1/3)</p>
                            <p className="mt-2 text-2xl font-bold text-blue-600">Rp {Math.floor(net / 3).toLocaleString('id-ID')}</p>
                        </div>

                        {/* Cici Linda */}
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-pink-500 flex flex-col gap-3">
                            <div>
                                <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Cici Linda (1/3)</p>
                                <p className="mt-1 text-xs text-gray-400 dark:text-gray-500">Siti Melinda Sari</p>
                                <p className="mt-2 text-2xl font-bold text-pink-600">Rp {Math.floor(net / 3).toLocaleString('id-ID')}</p>
                            </div>
                            <button
                                onClick={() => handleGenerateInvoice('Siti Melinda Sari', 'Cici Linda')}
                                disabled={generatingPdf !== null}
                                className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-pink-500 hover:bg-pink-600 disabled:opacity-60 text-white rounded-xl text-sm font-bold transition-all shadow-md hover:shadow-pink-200 dark:hover:shadow-pink-900"
                            >
                                {generatingPdf === 'Cici Linda' ? (
                                    <>
                                        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                                        </svg>
                                        Generating...
                                    </>
                                ) : (
                                    <>
                                        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3M3 17V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
                                        </svg>
                                        Generate Invoice PDF
                                    </>
                                )}
                            </button>
                        </div>

                        {/* Cici Zulfa */}
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-purple-500 flex flex-col gap-3">
                            <div>
                                <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Cici Zulfa (1/3)</p>
                                <p className="mt-1 text-xs text-gray-400 dark:text-gray-500">Zulfa Astri Lutfiah</p>
                                <p className="mt-2 text-2xl font-bold text-purple-600">Rp {Math.floor(net / 3).toLocaleString('id-ID')}</p>
                            </div>
                            <button
                                onClick={() => handleGenerateInvoice('Zulfa Astri Lutfiah', 'Cici Zulfa')}
                                disabled={generatingPdf !== null}
                                className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-purple-500 hover:bg-purple-600 disabled:opacity-60 text-white rounded-xl text-sm font-bold transition-all shadow-md hover:shadow-purple-200 dark:hover:shadow-purple-900"
                            >
                                {generatingPdf === 'Cici Zulfa' ? (
                                    <>
                                        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                                        </svg>
                                        Generating...
                                    </>
                                ) : (
                                    <>
                                        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3M3 17V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
                                        </svg>
                                        Generate Invoice PDF
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                )}

                {loading && <p className="text-center text-gray-500 animate-pulse">Calculating earnings...</p>}

            </div>
        </div>
    );
}
