'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { createTask } from '@/lib/firestore';
import { getTodayDate } from '@/lib/utils';


export default function NewNotePage() {
    const router = useRouter();
    const [loading, setLoading] = useState(false);
    const [form, setForm] = useState({
        date: getTodayDate(),
        appName: '', // This will hold the note content
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!form.appName.trim()) {
            alert('Note content is required');
            return;
        }

        setLoading(true);
        try {
            await createTask({
                date: form.date,
                appName: form.appName,
                taskType: 'NOTES',
                // Keep these empty as they are required by the type but irrelevant for notes
                packageName: '',
                playStoreUrl: '',
                acceptUrl: ''
            });
            // Redirect back to schedule instead of home since notes are schedule-based
            router.push('/schedule');
        } catch (err) {
            alert('Failed to save note');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
            <div className="max-w-2xl mx-auto px-4">
                <div className="flex items-center justify-between mb-6">
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Add Schedule Note</h1>
                    <Link href="/schedule" className="text-blue-600 hover:underline">Back to Schedule</Link>
                </div>

                <form onSubmit={handleSubmit} className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 space-y-4">
                    {/* Date */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date</label>
                        <input
                            type="date"
                            value={form.date}
                            onChange={(e) => setForm({ ...form, date: e.target.value })}
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none"
                            required
                        />
                    </div>

                    {/* Note Content */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Note Content
                        </label>
                        <textarea
                            value={form.appName}
                            onChange={(e) => setForm({ ...form, appName: e.target.value })}
                            placeholder="Enter your reminder or note here..."
                            className="w-full border rounded-lg px-3 py-2 dark:bg-gray-700 dark:border-gray-600 dark:text-white h-40 resize-y focus:ring-2 focus:ring-blue-500 outline-none"
                            required
                        />
                    </div>

                    {/* Submit */}
                    <div className="flex gap-4 pt-4">
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 bg-yellow-500 text-white font-semibold py-2 rounded-lg hover:bg-yellow-600 transition disabled:opacity-50"
                        >
                            {loading ? 'Saving...' : 'Save Note'}
                        </button>
                        <Link
                            href="/schedule"
                            className="flex-1 text-center font-medium border border-gray-300 py-2 rounded-lg hover:bg-gray-100 dark:border-gray-600 dark:hover:bg-gray-700 dark:text-gray-300"
                        >
                            Cancel
                        </Link>
                    </div>
                </form>
            </div>
        </div>
    );
}
