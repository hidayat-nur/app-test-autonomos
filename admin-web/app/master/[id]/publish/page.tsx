'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { getMasterAppById, updateMasterApp, createTask, type MasterApp } from '@/lib/firestore';
import { getTodayDate, addDays } from '@/lib/utils';
import { use } from 'react';


export default function PublishMasterAppPage({ params }: { params: Promise<{ id: string }> }) {
    const resolvedParams = use(params);
    const router = useRouter();
    const [appData, setAppData] = useState<MasterApp | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    // Form State
    const [publishDate, setPublishDate] = useState(getTodayDate());
    const [appName, setAppName] = useState('');
    const [packageName, setPackageName] = useState('');
    const [playStoreUrl, setPlayStoreUrl] = useState('');
    const [acceptUrl, setAcceptUrl] = useState('');
    const [credentials, setCredentials] = useState('');

    useEffect(() => {
        const loadApp = async () => {
            try {
                const data = await getMasterAppById(resolvedParams.id);
                if (data) {
                    setAppData(data);
                    setAppName(data.appName || '');
                    setPackageName(data.packageName || '');
                    setPlayStoreUrl(data.playStoreUrl || '');
                    setAcceptUrl(data.acceptUrl || '');
                    setCredentials(data.credentials || '');
                } else {
                    alert('App not found');
                    router.push('/');
                }
            } catch (err) {
                console.error(err);
                alert('Errors loading data');
            } finally {
                setLoading(false);
            }
        };
        loadApp();
    }, [resolvedParams.id, router]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!appData) return;

        if (!publishDate || !appName || !packageName || !playStoreUrl || !acceptUrl || !credentials) {
            alert("All technical fields and a Publish Date must be filled to publish this app.");
            return;
        }

        setSaving(true);
        try {
            // 1. Update Master App with technical details & mark PUBLISHED
            await updateMasterApp(appData.id!, {
                appName,
                packageName,
                playStoreUrl,
                acceptUrl,
                credentials,
                rateDate: addDays(publishDate, 12),
                deleteDate: addDays(publishDate, 19),
                status: 'PUBLISHED'
            });

            // 2. Auto-Schedule Tasks (+0, +12, +19)
            const commonTaskData = {
                appName,
                packageName,
                playStoreUrl,
                acceptUrl
            };

            await Promise.all([
                createTask({ ...commonTaskData, date: publishDate, taskType: 'TEST_APP' }),
                createTask({ ...commonTaskData, date: addDays(publishDate, 12), taskType: 'RATE_APP' }),
                createTask({ ...commonTaskData, date: addDays(publishDate, 19), taskType: 'DELETE_APP' })
            ]);

            alert(`Successfully published! Scheduled 3 tasks starting on ${publishDate}.`);
            router.push('/');
        } catch (err) {
            console.error(err);
            alert('Publishing failed.');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <div className="p-8 text-center bg-gray-50 flex items-center justify-center min-h-screen">Loading App Details...</div>;

    return (
        <div className="max-w-2xl mx-auto px-4 py-8">
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Publish Client App</h1>
                    <p className="text-gray-500">Add technical details to push <span className="font-bold text-blue-600">{appData?.clientName}</span> live.</p>
                </div>
                <Link href="/" className="text-blue-600 hover:underline">Cancel</Link>
            </div>

            <form onSubmit={handleSubmit} className="bg-white dark:bg-gray-800 shadow rounded-lg p-6 space-y-6">
                {/* Auto-Scheduling Notice */}
                <div className="bg-yellow-50 dark:bg-yellow-900/20 border-l-4 border-yellow-400 p-4 rounded">
                    <div className="flex">
                        <div className="ml-3">
                            <h3 className="text-sm font-medium text-yellow-800 dark:text-yellow-300">Auto-Scheduling Active</h3>
                            <div className="mt-2 text-sm text-yellow-700 dark:text-yellow-400">
                                Publishing this app will automatically schedule:
                                <ul className="list-disc ml-5 mt-1">
                                    <li><b>Test App</b> on {publishDate}</li>
                                    <li><b>Rate App</b> on {addDays(publishDate, 12)} (+12 days)</li>
                                    <li><b>Uninstall App</b> on {addDays(publishDate, 19)} (+19 days)</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Start / Publish Date *</label>
                        <input
                            type="date"
                            value={publishDate}
                            onChange={e => setPublishDate(e.target.value)}
                            required
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">App Name in Store *</label>
                        <input
                            type="text"
                            value={appName}
                            onChange={e => setAppName(e.target.value)}
                            required
                            placeholder="e.g. My Cool App"
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Package Name (ID) *</label>
                        <input
                            type="text"
                            value={packageName}
                            onChange={e => setPackageName(e.target.value)}
                            required
                            placeholder="e.g. com.example.app"
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Account Credentials *</label>
                        <input
                            type="text"
                            value={credentials}
                            onChange={e => setCredentials(e.target.value)}
                            required
                            placeholder="e.g. user:pass"
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
                        />
                    </div>

                    <div className="md:col-span-2">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Play Store URL *</label>
                        <input
                            type="url"
                            value={playStoreUrl}
                            onChange={e => setPlayStoreUrl(e.target.value)}
                            required
                            placeholder="https://play.google.com/..."
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
                        />
                    </div>

                    <div className="md:col-span-2">
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Accept URL (Internal Link) *</label>
                        <input
                            type="url"
                            value={acceptUrl}
                            onChange={e => setAcceptUrl(e.target.value)}
                            required
                            placeholder="https://..."
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
                        />
                    </div>

                </div>

                <div className="pt-4 border-t dark:border-gray-700">
                    <button
                        type="submit"
                        disabled={saving}
                        className="w-full bg-green-600 font-bold text-white py-3 rounded-lg hover:bg-green-700 transition disabled:opacity-50"
                    >
                        {saving ? 'Publishing & Scheduling...' : 'Publish & Start Schedule'}
                    </button>
                </div>
            </form>
        </div>
    );
}
