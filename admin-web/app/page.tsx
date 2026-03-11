'use client';

import { useState, useEffect } from 'react';
import { getMasterApps, updateMasterApp, createTask, deleteMasterAppAndTasks, type MasterApp, type MasterAppStatus, type TaskType } from '@/lib/firestore';
import Link from 'next/link';

function getTodayDate(): string {
    return new Date().toISOString().split('T')[0];
}

function formatDate(dateStr: string): string {
    const [year, month, day] = dateStr.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' });
}

function addDays(dateStr: string, days: number): string {
    const d = new Date(dateStr);
    d.setDate(d.getDate() + days);
    return d.toISOString().split('T')[0];
}

export default function MasterDashboard() {
    const [apps, setApps] = useState<MasterApp[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filter, setFilter] = useState<MasterAppStatus | 'ALL'>('ALL');
    const [search, setSearch] = useState('');
    const [publishModal, setPublishModal] = useState<MasterApp | null>(null);
    const [publishForm, setPublishForm] = useState({ appName: '', packageName: '', credentials: '', publishDate: getTodayDate() });
    const [publishing, setPublishing] = useState(false);

    const loadApps = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getMasterApps(filter === 'ALL' ? undefined : filter);
            // Hide ARCHIVED from the ALL tab — they have their own dedicated tab
            setApps(filter === 'ALL' ? data.filter(a => a.status !== 'ARCHIVED') : data);
        } catch (err) {
            console.error('Error loading Master Apps:', err);
            setError(err instanceof Error ? err.message : 'Failed to load Master Apps');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadApps();
    }, [filter]);

    const handleAction = async (app: MasterApp, actionType: 'DELETE_APP' | 'RATE_APP' | 'UPDATE_APP') => {
        if (app.status === 'DRAFT') {
            if (!window.confirm("Warning: This app hasn't been published yet (No URLs/Package Name). Are you sure you want to schedule this task?")) {
                return;
            }
        }

        const dateStr = window.prompt(`Enter Date for ${actionType} Task (YYYY-MM-DD):`, getTodayDate());
        if (!dateStr) return;

        if (!/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
            alert("Invalid date format. Please use YYYY-MM-DD.");
            return;
        }

        try {
            await createTask({
                date: dateStr,
                appName: app.appName || app.clientName || 'Unknown App',
                packageName: app.packageName || '',
                playStoreUrl: app.playStoreUrl || '',
                acceptUrl: app.acceptUrl || '',
                taskType: actionType
            });

            // If it's the uninstall task on a published app, mark it as deleted on the master list
            if (actionType === 'DELETE_APP') {
                await updateMasterApp(app.id!, { status: 'DELETED' });
                loadApps();
            }
            alert(`Task scheduled for ${dateStr}!`);
        } catch (err) {
            console.error(err);
            alert('Action failed.');
        }
    };

    const handleDestroyMaster = async (app: MasterApp) => {
        const confirmMsg = `DANGER: Are you sure you want to WIPE the master record for ${app.clientName}?\nThis will remove all Earning records and delete all related Test/Rate/Uninstall tasks from the calendar.`;
        if (!window.confirm(confirmMsg)) return;

        try {
            await deleteMasterAppAndTasks(app.id!, app.packageName);
            alert('Master record and all related tasks wiped successfully.');
            loadApps();
        } catch (e) {
            console.error(e);
            alert('Failed to destroy master record');
        }
    };

    const handleArchive = async (app: MasterApp) => {
        if (!window.confirm(`Archive "${app.clientName}"? App akan disembunyikan dari tab ALL/DRAFT/PUBLISHED.`)) return;
        try {
            await updateMasterApp(app.id!, { status: 'ARCHIVED' });
            loadApps();
        } catch (e) { console.error(e); alert('Gagal archive.'); }
    };

    const handleUnarchive = async (app: MasterApp) => {
        try {
            await updateMasterApp(app.id!, { status: 'DRAFT' });
            loadApps();
        } catch (e) { console.error(e); alert('Gagal unarchive.'); }
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
                credentials: publishForm.credentials || undefined,
                rateDate: addDays(publishDate, 12),
                deleteDate: addDays(publishDate, 19),
                status: 'PUBLISHED',
            });
            const commonTask = { appName, packageName, playStoreUrl, acceptUrl };
            await Promise.all([
                createTask({ ...commonTask, date: publishDate, taskType: 'TEST_APP' }),
                createTask({ ...commonTask, date: addDays(publishDate, 12), taskType: 'RATE_APP' }),
                createTask({ ...commonTask, date: addDays(publishDate, 19), taskType: 'DELETE_APP' }),
            ]);
            alert(`Berhasil publish! 3 task dijadwalkan mulai ${formatDate(publishDate)}.`);
            setPublishModal(null);
            loadApps();
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

    return (
        <>
            <div className="w-full px-[14px] py-8">
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Master Apps</h1>
                        <p className="text-gray-500 text-sm">Manage core client data and bulk-publish schedules.</p>
                    </div>
                    <div className="space-x-2">
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

                <div className="mb-6 flex space-x-2">
                    {(['ALL', 'DRAFT', 'PUBLISHED', 'ARCHIVED'] as const).map(status => (
                        <button
                            key={status}
                            onClick={() => setFilter(status)}
                            className={`px-4 py-2 rounded-lg text-sm font-medium transition ${filter === status
                                ? 'bg-blue-600 text-white shadow'
                                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border hover:bg-gray-50 dark:hover:bg-gray-700'
                                }`}
                        >
                            {status}
                        </button>
                    ))}
                </div>

                {loading && (
                    <div className="animate-pulse space-y-4">
                        {[1, 2, 3].map(i => (
                            <div key={i} className="h-16 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
                        ))}
                    </div>
                )}

                {error && <p className="text-red-500 bg-red-50 p-4 rounded">{error}</p>}

                {!loading && !error && (
                    <div className="bg-white dark:bg-gray-800 shadow rounded-lg overflow-hidden flex flex-col items-stretch overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                            <thead className="bg-gray-50 dark:bg-gray-700/50">
                                <tr>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Client / App Info</th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Platform & Earning</th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Status</th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Tgl Published</th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Jadwal Rating</th>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Jadwal Hapus</th>
                                    <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Manual Tasks</th>
                                    <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Master Actions</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                                {filteredApps.length === 0 ? (
                                    <tr>
                                        <td colSpan={8} className="px-6 py-12 text-center text-gray-500">
                                            No apps found for this filter.
                                        </td>
                                    </tr>
                                ) : (
                                    filteredApps.map(app => (
                                        <tr key={app.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition">
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <div className="text-sm font-medium text-gray-900 dark:text-white">{app.clientName} {app.appName ? `(${app.appName})` : ''}</div>
                                                <div className="text-xs text-gray-500">{app.packageName || 'No package'}</div>
                                                <div className="text-xs text-gray-400 mt-1 max-w-[200px] truncate">{app.credentials || 'No credentials'}</div>
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
                                                    {app.createdAt ? formatDate(new Date(app.createdAt).toISOString().split('T')[0]) : '-'}
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
