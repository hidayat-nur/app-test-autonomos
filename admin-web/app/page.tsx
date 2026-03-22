'use client';

import { useState, useEffect } from 'react';
import { getMasterApps, updateMasterApp, createTask, deleteMasterAppAndTasks, type MasterApp, type MasterAppStatus } from '@/lib/firestore';
import { getTodayDate, addDays, getLocalDateFromTimestamp } from '@/lib/utils';
import Link from 'next/link';


function formatDate(dateStr: string): string {
    const [year, month, day] = dateStr.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' });
}


export default function MasterDashboard() {
    const [apps, setApps] = useState<MasterApp[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filter, setFilter] = useState<MasterAppStatus | 'ALL' | 'RATE_TODAY' | 'DELETE_TODAY' | 'WARNING'>('ALL');
    const [selectedDate, setSelectedDate] = useState(getTodayDate());
    const [tabCounts, setTabCounts] = useState<Record<string, number>>({});
    const [search, setSearch] = useState('');
    const [publishModal, setPublishModal] = useState<MasterApp | null>(null);
    const [publishForm, setPublishForm] = useState({ appName: '', packageName: '', credentials: '', publishDate: getTodayDate() });
    const [publishing, setPublishing] = useState(false);

    const loadCounts = async () => {
        try {
            const today = getTodayDate();
            const all = await getMasterApps();
            setTabCounts({
                ALL: all.filter(a => a.status !== 'ARCHIVED').length,
                DRAFT: all.filter(a => a.status === 'DRAFT').length,
                PUBLISHED: all.filter(a => a.status === 'PUBLISHED').length,
                ARCHIVED: all.filter(a => a.status === 'ARCHIVED').length,
                RATE_TODAY: all.filter(a => a.rateDate === today && a.status !== 'DELETED' && a.status !== 'ARCHIVED').length,
                DELETE_TODAY: all.filter(a => a.deleteDate === today && a.status !== 'DELETED' && a.status !== 'ARCHIVED').length,
                WARNING: all.filter(a => a.warning && a.status !== 'DELETED').length,
            });
        } catch { /* silent */ }
    };

    const loadApps = async () => {
        setLoading(true);
        setError(null);
        try {
            if (filter === 'RATE_TODAY') {
                const data = await getMasterApps();
                setApps(data.filter(a => a.rateDate === selectedDate && a.status !== 'DELETED' && a.status !== 'ARCHIVED'));
            } else if (filter === 'DELETE_TODAY') {
                const data = await getMasterApps();
                setApps(data.filter(a => a.deleteDate === selectedDate && a.status !== 'DELETED' && a.status !== 'ARCHIVED'));
            } else if (filter === 'WARNING') {
                const data = await getMasterApps();
                setApps(data.filter(a => a.warning && a.status !== 'DELETED'));
            } else {
                const data = await getMasterApps(filter === 'ALL' ? undefined : filter as MasterAppStatus);
                setApps(filter === 'ALL' ? data.filter(a => a.status !== 'ARCHIVED') : data);
            }
        } catch (err) {
            console.error('Error loading Master Apps:', err);
            setError(err instanceof Error ? err.message : 'Failed to load Master Apps');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadApps();
    }, [filter, selectedDate]);

    useEffect(() => {
        loadCounts();
    }, []);

    const handleAction = (app: MasterApp, actionType: 'DELETE_APP' | 'RATE_APP' | 'UPDATE_APP') => {
        setActionDate(getTodayDate());
        setActionModal({ app, actionType });
    };

    const handleActionSubmit = async () => {
        if (!actionModal || !actionDate) return;
        const { app, actionType } = actionModal;

        if (app.status === 'DRAFT') {
            if (!window.confirm("Warning: This app hasn't been published yet. Lanjut?")) return;
        }

        setActionPushing(true);
        try {
            await createTask({
                date: actionDate,
                appName: app.appName || app.clientName || 'Unknown App',
                packageName: app.packageName || '',
                playStoreUrl: app.playStoreUrl || '',
                acceptUrl: app.acceptUrl || '',
                taskType: actionType,
            });
            if (actionType === 'DELETE_APP') {
                await updateMasterApp(app.id!, { status: 'ARCHIVED' });
            }
            alert(`Task dijadwalkan untuk ${formatDate(actionDate)}!`);
            setActionModal(null);
            loadApps(); loadCounts();
        } catch (err) {
            console.error(err);
            alert('Action gagal.');
        } finally {
            setActionPushing(false);
        }
    };

    const handleDestroyMaster = async (app: MasterApp) => {
        const confirmMsg = `DANGER: Are you sure you want to WIPE the master record for ${app.clientName}?\nThis will remove all Earning records and delete all related Test/Rate/Uninstall tasks from the calendar.`;
        if (!window.confirm(confirmMsg)) return;

        try {
            await deleteMasterAppAndTasks(app.id!, app.packageName);
            alert('Master record and all related tasks wiped successfully.');
            loadApps(); loadCounts();
        } catch (e) {
            console.error(e);
            alert('Failed to destroy master record');
        }
    };

    const [warningModal, setWarningModal] = useState<MasterApp | null>(null);
    const [warningNote, setWarningNote] = useState('');

    const handleSetWarning = async () => {
        if (!warningModal || !warningNote.trim()) return;
        try {
            await updateMasterApp(warningModal.id!, { warning: true, warningNote: warningNote.trim() });
            setWarningModal(null);
            loadApps();
        } catch (e) { console.error(e); alert('Gagal set warning.'); }
    };

    const handleClearWarning = async (app: MasterApp) => {
        try {
            await updateMasterApp(app.id!, { warning: false, warningNote: '' });
            loadApps();
        } catch (e) { console.error(e); alert('Gagal clear warning.'); }
    };

    const handleArchive = async (app: MasterApp) => {
        if (!window.confirm(`Archive "${app.clientName}"? App akan disembunyikan dari tab ALL/DRAFT/PUBLISHED.`)) return;
        try {
            await updateMasterApp(app.id!, { status: 'ARCHIVED' });
            loadApps(); loadCounts();
        } catch (e) { console.error(e); alert('Gagal archive.'); }
    };

    const handleUnarchive = async (app: MasterApp) => {
        try {
            await updateMasterApp(app.id!, { status: 'DRAFT' });
            loadApps(); loadCounts();
        } catch (e) { console.error(e); alert('Gagal unarchive.'); }
    };

    const [actionModal, setActionModal] = useState<{ app: MasterApp; actionType: 'DELETE_APP' | 'RATE_APP' | 'UPDATE_APP' } | null>(null);
    const [actionDate, setActionDate] = useState(getTodayDate());
    const [actionPushing, setActionPushing] = useState(false);

const [bulkPushing, setBulkPushing] = useState(false);
    const [bulkPushModal, setBulkPushModal] = useState(false);
    const [bulkPushDate, setBulkPushDate] = useState(getTodayDate());
    const [copyingUrls, setCopyingUrls] = useState(false);
    const [urlsCopied, setUrlsCopied] = useState(false);
    const [copyingAccept, setCopyingAccept] = useState(false);
    const [acceptCopied, setAcceptCopied] = useState(false);

    const handleCopyAcceptUrls = async () => {
        setCopyingAccept(true);
        try {
            const allApps = await getMasterApps();
            const withAccept = allApps.filter(a => a.status !== 'ARCHIVED' && a.acceptUrl);
            const urls = withAccept.map(a => `${a.acceptUrl}?authuser=2`).join('\n');
            if (!urls) { alert('Tidak ada app dengan accept URL (non-archived).'); return; }
            await navigator.clipboard.writeText(urls);
            setAcceptCopied(true);
            setTimeout(() => setAcceptCopied(false), 2000);
        } catch (e) {
            console.error(e);
            alert('Gagal copy accept URL.');
        } finally {
            setCopyingAccept(false);
        }
    };

    const handleCopyPlayStoreUrls = async () => {
        setCopyingUrls(true);
        try {
            const allApps = await getMasterApps();
            const nonArchived = allApps.filter(a => a.status !== 'ARCHIVED' && a.packageName);
            const urls = nonArchived.map(a => `https://play.google.com/store/apps/details?id=${a.packageName}`).join('\n');
            if (!urls) { alert('Tidak ada app dengan package name (non-archived).'); return; }
            await navigator.clipboard.writeText(urls);
            setUrlsCopied(true);
            setTimeout(() => setUrlsCopied(false), 2000);
        } catch (e) {
            console.error(e);
            alert('Gagal copy URL.');
        } finally {
            setCopyingUrls(false);
        }
    };

    const handleBulkPush = async () => {
        const taskType = filter === 'RATE_TODAY' ? 'RATE_APP' : 'DELETE_APP';
        const label = filter === 'RATE_TODAY' ? 'Rating' : 'Uninstall';
        setBulkPushModal(false);
        setBulkPushing(true);
        let done = 0;
        try {
            await Promise.all(
                filteredApps.map(app =>
                    createTask({
                        date: bulkPushDate,
                        appName: app.appName || app.clientName || 'Unknown',
                        packageName: app.packageName || '',
                        playStoreUrl: app.playStoreUrl || '',
                        acceptUrl: app.acceptUrl || '',
                        taskType,
                    }).then(() => done++)
                )
            );
            if (taskType === 'DELETE_APP') {
                await Promise.all(filteredApps.map(app => updateMasterApp(app.id!, { status: 'ARCHIVED' })));
            }
            alert(`Berhasil! ${done} task ${label} dijadwalkan untuk ${formatDate(bulkPushDate)}.`);
            loadApps(); loadCounts();
        } catch (e) {
            console.error(e);
            alert('Gagal push beberapa task.');
        } finally {
            setBulkPushing(false);
        }
    };

    const openPublishModal = (app: MasterApp) => {
        setPublishForm({
            appName: app.appName || '',
            packageName: app.packageName || '',
            credentials: app.credentials || '',
            publishDate: getTodayDate(),
        });
        setPublishModal(app);
    };

    const handlePublishSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!publishModal) return;
        const { appName, packageName, publishDate } = publishForm;
        if (!appName || !packageName || !publishDate) {
            alert('App Name, Package Name, dan Tanggal wajib diisi.');
            return;
        }
        const playStoreUrl = `https://play.google.com/store/apps/details?id=${packageName}`;
        const acceptUrl = `https://play.google.com/apps/testing/${packageName}`;
        setPublishing(true);
        try {
            await updateMasterApp(publishModal.id!, {
                appName,
                packageName,
                playStoreUrl,
                acceptUrl,
                ...(publishForm.credentials ? { credentials: publishForm.credentials } : {}),
                publishedAt: new Date(publishDate).getTime(),
                rateDate: addDays(publishDate, 12),
                deleteDate: addDays(publishDate, 19),
                status: 'PUBLISHED',
            });
            const commonTask = { appName, packageName, playStoreUrl, acceptUrl };
            await createTask({ ...commonTask, date: publishDate, taskType: 'TEST_APP' });
            alert(`Berhasil publish! Task TEST_APP dijadwalkan ${formatDate(publishDate)}. Dorong Rate & Uninstall secara manual.`);
            setPublishModal(null);
            loadApps(); loadCounts();
        } catch (err) {
            console.error(err);
            alert('Publish gagal.');
        } finally {
            setPublishing(false);
        }
    };

    const filteredApps = apps.filter(app => {
        if (!search.trim()) return true;
        const q = search.toLowerCase();
        return (
            (app.clientName || '').toLowerCase().includes(q) ||
            (app.appName || '').toLowerCase().includes(q) ||
            (app.packageName || '').toLowerCase().includes(q)
        );
    });

    type SortKey = 'clientName' | 'earning' | 'status' | 'createdAt' | 'publishedAt' | 'rateDate' | 'deleteDate';
    const [sortKey, setSortKey] = useState<SortKey>('createdAt');
    const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

    const handleSort = (key: SortKey) => {
        if (sortKey === key) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
        else { setSortKey(key); setSortDir('asc'); }
    };

    const sortedApps = [...filteredApps].sort((a, b) => {
        let av: string | number = '';
        let bv: string | number = '';
        if (sortKey === 'clientName') { av = (a.appName || a.clientName || '').toLowerCase(); bv = (b.appName || b.clientName || '').toLowerCase(); }
        else if (sortKey === 'earning') { av = a.earning || 0; bv = b.earning || 0; }
        else if (sortKey === 'status') { av = a.status || ''; bv = b.status || ''; }
        else if (sortKey === 'createdAt') { av = a.createdAt || 0; bv = b.createdAt || 0; }
        else if (sortKey === 'publishedAt') { av = a.publishedAt || 0; bv = b.publishedAt || 0; }
        else if (sortKey === 'rateDate') { av = a.rateDate || ''; bv = b.rateDate || ''; }
        else if (sortKey === 'deleteDate') { av = a.deleteDate || ''; bv = b.deleteDate || ''; }
        if (av < bv) return sortDir === 'asc' ? -1 : 1;
        if (av > bv) return sortDir === 'asc' ? 1 : -1;
        return 0;
    });

    const SortTh = ({ col, label, right }: { col: SortKey; label: string; right?: boolean }) => (
        <th
            scope="col"
            onClick={() => handleSort(col)}
            className={`px-6 py-3 text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer select-none hover:bg-gray-100 dark:hover:bg-gray-600 transition ${right ? 'text-right' : 'text-left'}`}
        >
            {label}{' '}
            <span className="inline-block w-3">
                {sortKey === col ? (sortDir === 'asc' ? '↑' : '↓') : <span className="opacity-25">↕</span>}
            </span>
        </th>
    );

    return (
        <>
            <div className="w-full px-[14px] py-8">
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Master Apps</h1>
                        <p className="text-gray-500 text-sm">Manage core client data and bulk-publish schedules.</p>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={handleCopyAcceptUrls}
                            disabled={copyingAccept}
                            title="Copy semua Accept/Testing URL dari data tersimpan (kecuali Archived)"
                            className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 transition disabled:opacity-50"
                        >
                            {acceptCopied ? '✓ Copied!' : copyingAccept ? 'Copying...' : '🔗 Copy Link Accept'}
                        </button>
                        <button
                            onClick={handleCopyPlayStoreUrls}
                            disabled={copyingUrls}
                            title="Copy semua URL Play Store testing (kecuali Archived)"
                            className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-green-700 transition disabled:opacity-50"
                        >
                            {urlsCopied ? '✓ Copied!' : copyingUrls ? 'Copying...' : '📋 Copy URL Play Store'}
                        </button>
<Link
                            href="/master/new"
                            className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition"
                        >
                            + Add Bulk Drafts
                        </Link>
                    </div>
                </div>

                <div className="mb-4">
                    <input
                        type="text"
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        placeholder="Cari client name, app name, atau package name..."
                        className="w-full px-4 py-2 border rounded-lg text-sm bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100 border-gray-300 dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                </div>

                <div className="mb-6 flex flex-wrap gap-2">
                    {(['ALL', 'DRAFT', 'PUBLISHED', 'ARCHIVED'] as const).map(status => (
                        <button
                            key={status}
                            onClick={() => setFilter(status)}
                            className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center gap-2 ${filter === status
                                ? 'bg-blue-600 text-white shadow'
                                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border hover:bg-gray-50 dark:hover:bg-gray-700'
                                }`}
                        >
                            {status}
                            {tabCounts[status] !== undefined && (
                                <span className={`text-xs px-1.5 py-0.5 rounded-full font-bold ${filter === status ? 'bg-white/25 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'}`}>
                                    {tabCounts[status]}
                                </span>
                            )}
                        </button>
                    ))}
                    <button
                        onClick={() => { setFilter('RATE_TODAY'); setSelectedDate(getTodayDate()); }}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center gap-2 ${filter === 'RATE_TODAY'
                            ? 'bg-yellow-500 text-white shadow'
                            : 'bg-white dark:bg-gray-800 text-yellow-600 border border-yellow-300 hover:bg-yellow-50'
                            }`}
                    >
                        ⭐ Rating
                        {tabCounts['RATE_TODAY'] !== undefined && (
                            <span className={`text-xs px-1.5 py-0.5 rounded-full font-bold ${filter === 'RATE_TODAY' ? 'bg-white/25 text-white' : 'bg-yellow-100 text-yellow-700'}`}>
                                {tabCounts['RATE_TODAY']}
                            </span>
                        )}
                    </button>
                    <button
                        onClick={() => { setFilter('DELETE_TODAY'); setSelectedDate(getTodayDate()); }}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center gap-2 ${filter === 'DELETE_TODAY'
                            ? 'bg-red-500 text-white shadow'
                            : 'bg-white dark:bg-gray-800 text-red-600 border border-red-300 hover:bg-red-50'
                            }`}
                    >
                        🗑️ Uninstall
                        {tabCounts['DELETE_TODAY'] !== undefined && (
                            <span className={`text-xs px-1.5 py-0.5 rounded-full font-bold ${filter === 'DELETE_TODAY' ? 'bg-white/25 text-white' : 'bg-red-100 text-red-700'}`}>
                                {tabCounts['DELETE_TODAY']}
                            </span>
                        )}
                    </button>
                    <button
                        onClick={() => setFilter('WARNING')}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center gap-2 ${filter === 'WARNING'
                            ? 'bg-orange-500 text-white shadow'
                            : 'bg-white dark:bg-gray-800 text-orange-600 border border-orange-300 hover:bg-orange-50'
                            }`}
                    >
                        ⚠️ Warning
                        {tabCounts['WARNING'] !== undefined && (
                            <span className={`text-xs px-1.5 py-0.5 rounded-full font-bold ${filter === 'WARNING' ? 'bg-white/25 text-white' : 'bg-orange-100 text-orange-700'}`}>
                                {tabCounts['WARNING']}
                            </span>
                        )}
                    </button>
                </div>

                {loading && (
                    <div className="animate-pulse space-y-4">
                        {[1, 2, 3].map(i => (
                            <div key={i} className="h-16 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
                        ))}
                    </div>
                )}

                {/* Bulk push banner for rate/uninstall tabs */}
                {(filter === 'RATE_TODAY' || filter === 'DELETE_TODAY') && !loading && (
                    <div className={`mb-4 p-4 rounded-lg border ${filter === 'RATE_TODAY' ? 'bg-yellow-50 border-yellow-200' : 'bg-red-50 border-red-200'}`}>
                        <div className="flex items-center justify-between gap-4 flex-wrap">
                            <div className="flex items-center gap-3 flex-wrap">
                                <div>
                                    <p className={`font-semibold text-sm ${filter === 'RATE_TODAY' ? 'text-yellow-800' : 'text-red-800'}`}>
                                        {filter === 'RATE_TODAY' ? '⭐ Rating' : '🗑️ Uninstall'} — {filteredApps.length} app terjadwal
                                    </p>
                                    <p className="text-xs text-gray-500 mt-0.5">
                                        {selectedDate === getTodayDate() ? 'Hari ini' : 'Tanggal dipilih'}
                                    </p>
                                </div>
                                <div className="flex items-center gap-2">
                                    <span className="text-xs text-gray-500">Lihat tgl:</span>
                                    <input
                                        type="date"
                                        value={selectedDate}
                                        onChange={e => setSelectedDate(e.target.value)}
                                        className={`border rounded-lg px-2 py-1 text-sm font-medium cursor-pointer focus:outline-none focus:ring-2 ${filter === 'RATE_TODAY'
                                            ? 'border-yellow-300 bg-yellow-100 text-yellow-800 focus:ring-yellow-400'
                                            : 'border-red-300 bg-red-100 text-red-800 focus:ring-red-400'}`}
                                    />
                                    {selectedDate !== getTodayDate() && (
                                        <button
                                            onClick={() => setSelectedDate(getTodayDate())}
                                            className="text-xs text-gray-500 underline hover:text-gray-700"
                                        >
                                            Reset ke hari ini
                                        </button>
                                    )}
                                </div>
                            </div>
                            <button
                                onClick={() => { setBulkPushDate(getTodayDate()); setBulkPushModal(true); }}
                                disabled={bulkPushing || filteredApps.length === 0}
                                className={`px-5 py-2 rounded-lg text-sm font-bold text-white transition disabled:opacity-50 ${filter === 'RATE_TODAY' ? 'bg-yellow-500 hover:bg-yellow-600' : 'bg-red-500 hover:bg-red-600'}`}
                            >
                                {bulkPushing ? 'Mendorong...' : `Dorong ${filteredApps.length} App →`}
                            </button>
                        </div>
                    </div>
                )}

                {error && <p className="text-red-500 bg-red-50 p-4 rounded">{error}</p>}

                {!loading && !error && (
                    <div className="bg-white dark:bg-gray-800 shadow rounded-lg overflow-hidden flex flex-col items-stretch overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                            <thead className="bg-gray-50 dark:bg-gray-700/50">
                                <tr>
                                    <SortTh col="clientName" label="Client / App Info" />
                                    <SortTh col="earning" label="Platform & Earning" />
                                    <SortTh col="status" label="Status" />
                                    <SortTh col="createdAt" label="Tgl Order" />
                                    <SortTh col="publishedAt" label="Tgl Published" />
                                    <SortTh col="rateDate" label="Jadwal Rating" />
                                    <SortTh col="deleteDate" label="Jadwal Hapus" />
                                    <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Manual Tasks</th>
                                    <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Master Actions</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                                {filteredApps.length === 0 ? (
                                    <tr>
                                        <td colSpan={9} className="px-6 py-12 text-center text-gray-500">
                                            No apps found for this filter.
                                        </td>
                                    </tr>
                                ) : (
                                    sortedApps.map(app => (
                                        <tr key={app.id} className={`transition ${app.warning ? 'bg-orange-100 dark:bg-orange-900/40 border-l-4 border-orange-400 hover:bg-orange-200 dark:hover:bg-orange-900/60' : 'hover:bg-gray-50 dark:hover:bg-gray-700/50'}`}>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <div className="text-sm font-medium text-gray-900 dark:text-white flex items-center gap-1 group">
                                                    <span>{app.clientName}</span>
                                                    {app.appName && <span className="text-gray-500 font-normal">({app.appName})</span>}
                                                    <button
                                                        onClick={() => {
                                                            navigator.clipboard.writeText(app.clientName || '');
                                                            const btn = document.getElementById(`copy-${app.id}`);
                                                            if (btn) { btn.textContent = '✓'; setTimeout(() => { btn.textContent = '📋'; }, 1200); }
                                                        }}
                                                        id={`copy-${app.id}`}
                                                        title="Copy client name"
                                                        className="opacity-30 group-hover:opacity-100 text-gray-400 hover:text-blue-500 transition text-base px-1"
                                                    >📋</button>
                                                </div>
                                                <div className="text-xs text-gray-500">{app.packageName || 'No package'}</div>
                                                <div className="text-xs text-gray-400 mt-1 max-w-[200px] truncate">{app.credentials || 'No credentials'}</div>
                                                {app.warning && (
                                                    <div className="mt-1.5 flex items-start gap-1 max-w-[220px]">
                                                        <span className="text-orange-500 text-xs font-bold shrink-0">⚠️</span>
                                                        <span className="text-xs text-orange-700 dark:text-orange-400 break-words">{app.warningNote}</span>
                                                    </div>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <div className="text-sm text-gray-900 dark:text-white capitalize">{app.platform}</div>
                                                <div className="text-sm font-bold text-green-600">Rp {app.earning.toLocaleString('id-ID')}</div>
                                                <div className="text-xs text-gray-500">{app.deviceCount} Devices</div>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                        ${app.status === 'PUBLISHED' ? 'bg-green-100 text-green-800' :
                                                        app.status === 'DELETED' ? 'bg-gray-100 text-gray-800' :
                                                            app.status === 'ARCHIVED' ? 'bg-purple-100 text-purple-800' :
                                                                'bg-yellow-100 text-yellow-800'}`}>
                                                    {app.status}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <span className="text-sm text-gray-600 dark:text-gray-300">
                                                    {app.createdAt ? formatDate(getLocalDateFromTimestamp(app.createdAt)) : '-'}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <span className="text-sm text-gray-600 dark:text-gray-300">
                                                    {app.publishedAt ? formatDate(getLocalDateFromTimestamp(app.publishedAt)) : <span className="text-gray-300">—</span>}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                {app.rateDate ? (
                                                    <div className={`px-3 py-1.5 rounded-md text-sm font-medium inline-block
                                                    ${app.rateDate < getTodayDate() ? 'bg-red-100 text-red-800 border border-red-200' :
                                                            app.rateDate === getTodayDate() ? 'bg-yellow-100 text-yellow-800 border border-yellow-200' :
                                                                'text-gray-600 dark:text-gray-300'
                                                        }
                                                `}>
                                                        {formatDate(app.rateDate)}
                                                    </div>
                                                ) : (
                                                    <span className="text-gray-400 text-sm">-</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                {app.deleteDate ? (
                                                    <div className={`px-3 py-1.5 rounded-md text-sm font-medium inline-block
                                                    ${app.deleteDate < getTodayDate() ? 'bg-red-100 text-red-800 border border-red-200' :
                                                            app.deleteDate === getTodayDate() ? 'bg-yellow-100 text-yellow-800 border border-yellow-200' :
                                                                'text-gray-600 dark:text-gray-300'
                                                        }
                                                `}>
                                                        {formatDate(app.deleteDate)}
                                                    </div>
                                                ) : (
                                                    <span className="text-gray-400 text-sm">-</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-2">
                                                {app.status === 'DRAFT' && (
                                                    <button
                                                        onClick={() => openPublishModal(app)}
                                                        className="inline-block text-blue-600 font-bold hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300"
                                                    >
                                                        🚀 PUBLISH
                                                    </button>
                                                )}
                                                {(app.status === 'PUBLISHED' || app.status === 'ARCHIVED') && (
                                                    <button
                                                        onClick={() => openPublishModal(app)}
                                                        className="inline-block text-indigo-600 font-bold hover:text-indigo-900 dark:text-indigo-400 dark:hover:text-indigo-300"
                                                    >
                                                        🔄 Re-Publish
                                                    </button>
                                                )}
                                                <button
                                                    onClick={() => handleAction(app, 'UPDATE_APP')}
                                                    className="text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300"
                                                >
                                                    +Update
                                                </button>
                                                <button
                                                    onClick={() => handleAction(app, 'RATE_APP')}
                                                    className="text-yellow-600 hover:text-yellow-900 dark:text-yellow-400 dark:hover:text-yellow-300"
                                                >
                                                    +Rate
                                                </button>
                                                <button
                                                    onClick={() => handleAction(app, 'DELETE_APP')}
                                                    className="text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300"
                                                >
                                                    +Uninstall
                                                </button>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right font-medium space-y-1">
                                                {app.status !== 'ARCHIVED' && (
                                                    <button
                                                        onClick={() => handleArchive(app)}
                                                        className="block w-full bg-orange-50 text-orange-600 px-3 py-1 rounded text-xs hover:bg-orange-100 dark:bg-orange-900/30 dark:text-orange-400 transition"
                                                    >
                                                        📦 Archive
                                                    </button>
                                                )}
                                                {app.status === 'ARCHIVED' && (
                                                    <button
                                                        onClick={() => handleUnarchive(app)}
                                                        className="block w-full bg-blue-50 text-blue-600 px-3 py-1 rounded text-xs hover:bg-blue-100 dark:bg-blue-900/30 dark:text-blue-400 transition"
                                                    >
                                                        ↩ Unarchive
                                                    </button>
                                                )}
                                                {app.warning ? (
                                                    <button
                                                        onClick={() => handleClearWarning(app)}
                                                        className="block w-full bg-orange-50 text-orange-600 px-3 py-1 rounded text-xs hover:bg-orange-100 dark:bg-orange-900/30 dark:text-orange-400 transition"
                                                    >
                                                        ✅ Unwarning
                                                    </button>
                                                ) : (
                                                    <button
                                                        onClick={() => { setWarningNote(''); setWarningModal(app); }}
                                                        className="block w-full bg-yellow-50 text-yellow-700 px-3 py-1 rounded text-xs hover:bg-yellow-100 dark:bg-yellow-900/30 dark:text-yellow-400 transition"
                                                    >
                                                        ⚠️ Warning
                                                    </button>
                                                )}
                                                <button
                                                    onClick={() => handleDestroyMaster(app)}
                                                    className="block w-full bg-red-50 text-red-600 px-3 py-1 rounded text-xs hover:bg-red-100 dark:bg-red-900/30 dark:text-red-400 transition"
                                                >
                                                    Wipe Master
                                                </button>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Warning Modal */}
            {warningModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-sm mx-4 p-6">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="text-lg font-bold text-gray-900 dark:text-white">⚠️ Set Warning</h2>
                            <button onClick={() => setWarningModal(null)} className="text-gray-400 hover:text-gray-600 text-xl font-bold">✕</button>
                        </div>
                        <p className="text-sm text-gray-500 mb-1">App:</p>
                        <p className="text-sm font-semibold text-blue-600 mb-4 truncate">
                            {warningModal.appName || warningModal.clientName}
                        </p>
                        <div className="mb-5">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Info Warning *</label>
                            <textarea
                                value={warningNote}
                                onChange={e => setWarningNote(e.target.value)}
                                placeholder="Contoh: Rating ditolak, cek kebijakan Play Store..."
                                rows={3}
                                className="w-full border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-400 resize-none"
                            />
                        </div>
                        <div className="flex gap-3">
                            <button
                                onClick={() => setWarningModal(null)}
                                className="flex-1 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 py-2 rounded-lg text-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition"
                            >
                                Batal
                            </button>
                            <button
                                onClick={handleSetWarning}
                                disabled={!warningNote.trim()}
                                className="flex-1 bg-orange-500 hover:bg-orange-600 font-bold py-2 rounded-lg text-sm text-white transition disabled:opacity-50"
                            >
                                Set Warning
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Single Action Date Modal */}
            {actionModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-sm mx-4 p-6">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                                {actionModal.actionType === 'RATE_APP' ? '⭐ Jadwalkan Rating' : actionModal.actionType === 'DELETE_APP' ? '🗑️ Jadwalkan Uninstall' : '🔄 Jadwalkan Update'}
                            </h2>
                            <button onClick={() => setActionModal(null)} className="text-gray-400 hover:text-gray-600 text-xl font-bold">✕</button>
                        </div>
                        <p className="text-sm text-gray-500 mb-1">App:</p>
                        <p className="text-sm font-semibold text-blue-600 mb-4 truncate">
                            {actionModal.app.appName || actionModal.app.clientName}
                        </p>
                        <div className="mb-5">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tanggal Task *</label>
                            <input
                                type="date"
                                value={actionDate}
                                onChange={e => setActionDate(e.target.value)}
                                className="w-full border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                            <p className="text-xs text-gray-400 mt-1">{actionDate ? formatDate(actionDate) : ''}</p>
                        </div>
                        <div className="flex gap-3">
                            <button
                                onClick={() => setActionModal(null)}
                                className="flex-1 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 py-2 rounded-lg text-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition"
                            >
                                Batal
                            </button>
                            <button
                                onClick={handleActionSubmit}
                                disabled={!actionDate || actionPushing}
                                className={`flex-1 font-bold py-2 rounded-lg text-sm text-white transition disabled:opacity-50
                                    ${actionModal.actionType === 'RATE_APP' ? 'bg-yellow-500 hover:bg-yellow-600' :
                                    actionModal.actionType === 'DELETE_APP' ? 'bg-red-500 hover:bg-red-600' :
                                    'bg-green-500 hover:bg-green-600'}`}
                            >
                                {actionPushing ? 'Menyimpan...' : 'Jadwalkan →'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Bulk Push Date Modal */}
            {bulkPushModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-sm mx-4 p-6">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                                {filter === 'RATE_TODAY' ? '⭐ Dorong ke Rating' : '🗑️ Dorong ke Uninstall'}
                            </h2>
                            <button onClick={() => setBulkPushModal(false)} className="text-gray-400 hover:text-gray-600 text-xl font-bold">✕</button>
                        </div>
                        <p className="text-sm text-gray-500 mb-4">
                            {filteredApps.length} app akan dijadwalkan. Pilih tanggal tujuan task:
                        </p>
                        <div className="mb-5">
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tanggal Dorong *</label>
                            <input
                                type="date"
                                value={bulkPushDate}
                                onChange={e => setBulkPushDate(e.target.value)}
                                className="w-full border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                            <p className="text-xs text-gray-400 mt-1">{bulkPushDate ? formatDate(bulkPushDate) : ''}</p>
                        </div>
                        <div className="flex gap-3">
                            <button
                                onClick={() => setBulkPushModal(false)}
                                className="flex-1 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 py-2 rounded-lg text-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition"
                            >
                                Batal
                            </button>
                            <button
                                onClick={handleBulkPush}
                                disabled={!bulkPushDate}
                                className={`flex-1 font-bold py-2 rounded-lg text-sm text-white transition disabled:opacity-50 ${filter === 'RATE_TODAY' ? 'bg-yellow-500 hover:bg-yellow-600' : 'bg-red-500 hover:bg-red-600'}`}
                            >
                                Dorong {filteredApps.length} App →
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Publish Modal */}
            {
                publishModal && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-md mx-4 p-6">
                            <div className="flex items-center justify-between mb-4">
                                <div>
                                    <h2 className="text-lg font-bold text-gray-900 dark:text-white">🚀 Publish App</h2>
                                    <p className="text-sm text-gray-500">Client: <span className="font-semibold text-blue-600">{publishModal.clientName}</span></p>
                                </div>
                                <button onClick={() => setPublishModal(null)} className="text-gray-400 hover:text-gray-600 text-xl font-bold">✕</button>
                            </div>

                            <form onSubmit={handlePublishSubmit} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tanggal Publish *</label>
                                    <input
                                        type="date"
                                        value={publishForm.publishDate}
                                        onChange={e => setPublishForm(f => ({ ...f, publishDate: e.target.value }))}
                                        required
                                        className="w-full border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                                    />
                                    <p className="text-xs text-gray-400 mt-1">
                                        Rate: {formatDate(addDays(publishForm.publishDate, 12))} · Hapus: {formatDate(addDays(publishForm.publishDate, 19))}
                                    </p>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">App Name *</label>
                                    <input
                                        type="text"
                                        value={publishForm.appName}
                                        onChange={e => setPublishForm(f => ({ ...f, appName: e.target.value }))}
                                        required
                                        placeholder="e.g. My Cool App"
                                        className="w-full border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Package Name *</label>
                                    <input
                                        type="text"
                                        value={publishForm.packageName}
                                        onChange={e => setPublishForm(f => ({ ...f, packageName: e.target.value }))}
                                        required
                                        placeholder="e.g. com.example.app"
                                        className="w-full border rounded-lg px-3 py-2 text-sm font-mono dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                                    />
                                    {publishForm.packageName && (
                                        <p className="text-xs text-gray-400 mt-1 truncate">🔗 play.google.com/store/apps/details?id={publishForm.packageName}</p>
                                    )}
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Credential <span className="text-gray-400">(opsional)</span></label>
                                    <input
                                        type="text"
                                        value={publishForm.credentials}
                                        onChange={e => setPublishForm(f => ({ ...f, credentials: e.target.value }))}
                                        placeholder="e.g. user@email.com / password"
                                        className="w-full border rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                                    />
                                </div>
                                <div className="flex gap-3 pt-2">
                                    <button
                                        type="button"
                                        onClick={() => setPublishModal(null)}
                                        className="flex-1 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 py-2 rounded-lg text-sm hover:bg-gray-50 dark:hover:bg-gray-700 transition"
                                    >
                                        Batal
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={publishing}
                                        className="flex-1 bg-green-600 text-white font-bold py-2 rounded-lg text-sm hover:bg-green-700 transition disabled:opacity-50"
                                    >
                                        {publishing ? 'Publishing...' : '🚀 Publish & Schedule'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )
            }
        </>
    );
}
