'use client';

import { useState, useEffect, useCallback } from 'react';
import { Noto_Sans_Javanese } from 'next/font/google';
import { getMasterApps, getOperationalCosts, addOperationalCost, deleteOperationalCost, type MasterApp, type OperationalCost } from '@/lib/firestore';
import { toAksara } from '@/lib/aksara';

// Javanese webfont — the browser shapes the script correctly when we
// rasterise the invoice with html2canvas. `variable` keeps it opt-in so the
// dashboard UI itself is unaffected.
const jawaFont = Noto_Sans_Javanese({ weight: ['400', '700'], subsets: ['javanese'], display: 'swap', variable: '--font-jawa' });

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
    const [{ jsPDF }, { default: html2canvas }] = await Promise.all([
        import('jspdf'),
        import('html2canvas-pro'),
    ]);

    const invoiceNo = generateInvoiceNumber(data.recipientName, data.period);
    const today = new Date();
    const dateStr = today.toLocaleDateString('id-ID', { day: '2-digit', month: 'long', year: 'numeric' });

    const jv = (t: string) => toAksara(t);      // labels → Aksara Jawa
    const money = (n: number) => formatRp(n);    // nominal amounts stay Latin
    const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    // Palette — hex only (keeps html2canvas-pro happy; no oklch).
    const C = {
        slate: '#0f172a', slate600: '#475569', slate500: '#64748b', slate400: '#94a3b8',
        green: '#22c55e', green50: '#f0fdf4', green200: '#bbf7d0', green800: '#166534',
        red50: '#fef2f2', red700: '#b91c1c', ops: '#c2410c',
        line: '#e2e8f0', slate50: '#f8fafc', white: '#ffffff',
    };
    const family = jawaFont.style.fontFamily;

    // Operational-cost rows (itemised, or a single fallback row).
    const opsSource = data.opsList.length > 0
        ? data.opsList.map(op => ({ label: op.name, amount: op.amount }))
        : [{ label: 'Biaya Operasional', amount: data.totalOps }];
    const opsRows = opsSource.map(o => `
        <div class="brk" style="display:flex;justify-content:space-between;align-items:center;padding:7px 14px;border-bottom:1px solid ${C.line};color:${C.ops};font-size:13px;">
            <span style="padding-left:10px;">${jv(esc(o.label))}</span>
            <span style="font-weight:700;white-space:nowrap;">- ${money(o.amount)}</span>
        </div>`).join('');

    const sigBox = (label: string, name: string) => `
        <div style="flex:1;border:1px solid ${C.line};border-radius:8px;background:#fcfcfd;padding:10px 8px 12px;text-align:center;">
            <div style="font-size:11px;color:${C.slate500};margin-bottom:26px;">${jv(esc(label))}</div>
            <div style="border-top:1px solid ${C.slate400};margin:0 8px 6px;"></div>
            <div style="font-size:12px;font-weight:700;color:${C.slate};">${jv(esc(name))}</div>
        </div>`;

    const html = `
    <div style="width:794px;background:${C.white};color:${C.slate};font-family:${family};">
        <div class="brk" style="background:${C.slate};padding:22px 24px;display:flex;justify-content:space-between;align-items:flex-start;">
            <div>
                <div style="font-size:22px;font-weight:700;color:#fff;">${jv('PT Border Tech Indonesia')}</div>
                <div style="font-size:11px;color:${C.slate400};margin-top:6px;">${jv('Sistem Manajemen Aplikasi dan Penghasilan')}</div>
                <div style="font-size:11px;color:${C.slate400};margin-top:2px;">borderpedia.id</div>
            </div>
            <div style="text-align:right;">
                <div style="font-size:30px;font-weight:700;color:${C.green};line-height:1;">${jv('FAKTUR')}</div>
                <div style="font-size:11px;color:${C.slate400};margin-top:8px;">${jv('Nomor')} &nbsp;${invoiceNo}</div>
                <div style="font-size:11px;color:${C.slate400};margin-top:2px;">${jv('Tanggal')} &nbsp;${jv(dateStr)}</div>
            </div>
        </div>
        <div style="height:3px;background:${C.green};"></div>

        <div style="padding:22px 24px 0;">
            <div class="brk" style="display:flex;gap:12px;margin-bottom:18px;">
                <div style="flex:1;border:1px solid ${C.line};border-radius:8px;background:${C.slate50};padding:12px 14px;">
                    <div style="font-size:10px;font-weight:700;color:${C.slate500};">${jv('DITUJUKAN KEPADA')}</div>
                    <div style="font-size:16px;font-weight:700;margin-top:6px;">${jv(esc(data.recipientName))}</div>
                    <div style="font-size:12px;color:${C.slate600};margin-top:4px;">(${jv(esc(data.recipientNickname))}) &middot; ${jv('Staf')}</div>
                </div>
                <div style="flex:1;border:1px solid ${C.green200};border-radius:8px;background:${C.green50};padding:12px 14px;">
                    <div style="font-size:10px;font-weight:700;color:${C.slate500};">${jv('PERIODE PEMBAYARAN')}</div>
                    <div style="font-size:16px;font-weight:700;margin-top:6px;">${jv(esc(data.periodLabel))}</div>
                    <div style="font-size:12px;color:${C.slate600};margin-top:4px;">${jv('Bagi hasil bulanan')} &middot; ${jv('1/3 pendapatan bersih')}</div>
                </div>
            </div>

            <div class="brk" style="display:flex;justify-content:space-between;background:${C.slate};color:#fff;padding:8px 14px;border-radius:6px 6px 0 0;font-size:11px;font-weight:700;">
                <span>${jv('KETERANGAN')}</span><span>${jv('JUMLAH')}</span>
            </div>
            <div class="brk" style="display:flex;justify-content:space-between;padding:8px 14px;background:${C.slate50};border-bottom:1px solid ${C.line};font-weight:700;font-size:13px;">
                <span>${jv('Total Pendapatan Kotor (Bruto)')}</span><span style="white-space:nowrap;">${money(data.gross)}</span>
            </div>
            ${opsRows}
            <div class="brk" style="display:flex;justify-content:space-between;padding:8px 14px;background:${C.red50};color:${C.red700};border-bottom:1px solid ${C.line};font-weight:700;font-size:13px;">
                <span>${jv('Total Biaya Operasional')}</span><span style="white-space:nowrap;">- ${money(data.totalOps)}</span>
            </div>
            <div class="brk" style="display:flex;justify-content:space-between;padding:8px 14px;background:${C.green50};color:${C.green800};font-weight:700;font-size:13px;">
                <span>${jv('Pendapatan Bersih (setelah biaya operasional)')}</span><span style="white-space:nowrap;">${money(data.net)}</span>
            </div>

            <div class="brk" style="display:flex;justify-content:space-between;align-items:center;background:${C.green800};color:#fff;border-radius:8px;padding:11px 14px;margin-top:14px;">
                <span style="font-weight:700;font-size:14px;">${jv('Bagian')} ${jv(esc(data.recipientNickname))} (1/3 ${jv('Pendapatan Bersih')})</span>
                <span style="font-weight:700;font-size:16px;white-space:nowrap;">${money(data.share)}</span>
            </div>

            <div class="brk" style="display:flex;justify-content:space-between;align-items:center;background:${C.slate};border-radius:10px;padding:16px 18px;margin-top:12px;">
                <span style="font-size:14px;font-weight:700;color:#fff;">${jv(esc(data.recipientName))}</span>
                <span style="text-align:right;">
                    <span style="display:block;font-size:11px;color:${C.slate400};">${jv('TOTAL YANG DITERIMA')}</span>
                    <span style="display:block;font-size:24px;font-weight:700;color:${C.green};white-space:nowrap;">${money(data.share)}</span>
                </span>
            </div>

            <div class="brk" style="display:flex;gap:12px;margin-top:26px;">
                ${sigBox('Dibuat oleh', 'Bos Nur')}
                ${sigBox('Disetujui oleh', 'Bos Nur')}
                ${sigBox('Diterima oleh', data.recipientNickname)}
            </div>

            <div class="brk" style="margin-top:26px;border-top:2px solid ${C.green};padding-top:8px;padding-bottom:18px;display:flex;justify-content:space-between;font-size:9px;color:${C.slate500};">
                <span>${invoiceNo} &middot; ${jv('Dokumen dibuat otomatis pada')} ${jv(dateStr)} &middot; ${jv('PT Border Tech Indonesia')}</span>
                <span>borderpedia.id</span>
            </div>
        </div>
    </div>`;

    // ── Render offscreen so the browser shapes the Javanese script ──
    const host = document.createElement('div');
    host.style.cssText = 'position:fixed;left:-10000px;top:0;z-index:-1;';
    host.innerHTML = html;
    document.body.appendChild(host);
    const root = host.firstElementChild as HTMLElement;

    // Wait for the Javanese webfont, otherwise glyphs render as boxes.
    try {
        await Promise.all([
            document.fonts.load(`700 16px ${family}`),
            document.fonts.load(`400 13px ${family}`),
        ]);
        await document.fonts.ready;
    } catch { /* Font Loading API is best-effort */ }

    const canvas = await html2canvas(root, {
        scale: 2, backgroundColor: '#ffffff', useCORS: true, logging: false,
    });

    // Break candidates at block boundaries so no row is sliced mid-line.
    const ratio = canvas.width / root.offsetWidth;
    const rootTop = root.getBoundingClientRect().top;
    const breaks = Array.from(root.querySelectorAll('.brk'))
        .map(el => (el.getBoundingClientRect().top - rootTop) * ratio)
        .filter(v => v > 1);
    document.body.removeChild(host);

    // ── Assemble the PDF, paginating at safe break points ──
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const pageWmm = doc.internal.pageSize.getWidth();
    const pageHmm = doc.internal.pageSize.getHeight();
    const pxPerMm = canvas.width / pageWmm;
    const pageHpx = Math.floor(pageHmm * pxPerMm);

    const slices: Array<[number, number]> = [];
    let start = 0;
    while (start < canvas.height) {
        let end = Math.min(start + pageHpx, canvas.height);
        if (end < canvas.height) {
            const safe = breaks.filter(b => b > start + pageHpx * 0.3 && b <= end).pop();
            if (safe) end = safe;
        }
        end = Math.ceil(end);
        slices.push([start, end]);
        start = end;
    }

    slices.forEach(([s, e], idx) => {
        const h = e - s;
        const slice = document.createElement('canvas');
        slice.width = canvas.width;
        slice.height = h;
        const ctx = slice.getContext('2d')!;
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, slice.width, slice.height);
        ctx.drawImage(canvas, 0, s, canvas.width, h, 0, 0, canvas.width, h);
        const hmm = h / pxPerMm;
        if (idx > 0) doc.addPage();
        doc.addImage(slice.toDataURL('image/png'), 'PNG', 0, 0, pageWmm, hmm);
        if (slices.length > 1) {
            doc.setFont('helvetica', 'normal');
            doc.setFontSize(8);
            doc.setTextColor(148, 163, 184);
            doc.text(`${idx + 1} / ${slices.length}`, pageWmm - 8, pageHmm - 5, { align: 'right' });
        }
    });

    doc.save(`${invoiceNo}.pdf`);
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
        <div className={`${jawaFont.variable} min-h-screen bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8`}>
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
