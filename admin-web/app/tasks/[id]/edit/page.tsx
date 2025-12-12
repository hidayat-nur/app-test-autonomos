'use client';

import { useState, useEffect, use } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { getTaskById, updateTask, type TaskType } from '@/lib/firestore';
import { extractPackageName } from '@/lib/utils';

export default function EditTaskPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = use(params);
    const router = useRouter();
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [form, setForm] = useState({
        date: '',
        appName: '',
        taskType: 'DELETE_APP' as TaskType,
        playStoreUrl: '',
        acceptUrl: '',
    });

    useEffect(() => {
        const loadTask = async () => {
            try {
                const task = await getTaskById(id);
                if (task) {
                    setForm({
                        date: task.date,
                        appName: task.appName,
                        taskType: task.taskType,
                        playStoreUrl: task.playStoreUrl,
                        acceptUrl: task.acceptUrl,
                    });
                } else {
                    alert('Task not found');
                    router.push('/');
                }
            } catch (err) {
                alert('Failed to load task');
                router.push('/');
            } finally {
                setLoading(false);
            }
        };
        loadTask();
    }, [id, router]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // Validation
        if (!form.appName) {
            alert('App Name is required');
            return;
        }
        if (!form.playStoreUrl) {
            alert('Play Store URL is required');
            return;
        }
        if (form.taskType === 'TEST_APP' && !form.acceptUrl) {
            alert('Accept URL is required for Test App');
            return;
        }

        const pkgName = extractPackageName(form.playStoreUrl);
        if (!pkgName) {
            alert('Could not extract Package Name from Play Store URL');
            return;
        }

        setSaving(true);
        try {
            await updateTask(id, {
                ...form,
                packageName: pkgName,
            });
            router.push('/');
        } catch (err) {
            alert('Failed to update task');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
                <p className="text-gray-500">Loading...</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
            <div className="max-w-2xl mx-auto px-4">
                <div className="flex items-center justify-between mb-6">
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Edit Task</h1>
                    <Link href="/" className="text-blue-600 hover:underline">Back</Link>
                </div>

                <form onSubmit={handleSubmit} className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 space-y-4">
                    {/* Date */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date</label>
                        <input
                            type="date"
                            value={form.date}
                            onChange={(e) => setForm({ ...form, date: e.target.value })}
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                            required
                        />
                    </div>

                    {/* Task Type */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Task Type</label>
                        <select
                            value={form.taskType}
                            onChange={(e) => setForm({ ...form, taskType: e.target.value as TaskType })}
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                        >
                            <option value="DELETE_APP">Hapus App</option>
                            <option value="RATE_APP">Rating App</option>
                            <option value="TEST_APP">Test App Baru</option>
                            <option value="UPDATE_APP">Update App</option>
                        </select>
                    </div>

                    {/* App Name */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">App Name</label>
                        <input
                            type="text"
                            value={form.appName}
                            onChange={(e) => setForm({ ...form, appName: e.target.value })}
                            placeholder="e.g. My App"
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                            required
                        />
                    </div>



                    {/* Play Store URL */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Play Store URL <span className="text-red-500">*</span></label>
                        <input
                            type="url"
                            value={form.playStoreUrl}
                            onChange={(e) => setForm({ ...form, playStoreUrl: e.target.value })}
                            placeholder="https://play.google.com/store/apps/details?id=..."
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                            required
                        />
                    </div>

                    {/* Accept URL (only for TEST_APP) */}
                    {form.taskType === 'TEST_APP' && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Accept URL</label>
                            <input
                                type="url"
                                value={form.acceptUrl}
                                onChange={(e) => setForm({ ...form, acceptUrl: e.target.value })}
                                placeholder="https://..."
                                className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                            />
                        </div>
                    )}

                    {/* Submit */}
                    <div className="flex gap-4 pt-4">
                        <button
                            type="submit"
                            disabled={saving}
                            className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
                        >
                            {saving ? 'Saving...' : 'Update Task'}
                        </button>
                        <Link
                            href="/"
                            className="flex-1 text-center border border-gray-300 py-2 rounded-lg hover:bg-gray-100 dark:border-gray-600 dark:hover:bg-gray-700"
                        >
                            Cancel
                        </Link>
                    </div>
                </form>
            </div>
        </div>
    );
}
