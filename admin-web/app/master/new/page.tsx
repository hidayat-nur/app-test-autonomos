'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { createMasterApp } from '@/lib/firestore';

export default function NewMasterAppPage() {
    const router = useRouter();
    const [loading, setLoading] = useState(false);
    const [bulkInput, setBulkInput] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // format: ClientName,Platform,Devices,Earning
        const lines = bulkInput.split('\n').map(line => line.trim()).filter(line => line.length > 0);

        if (lines.length === 0) {
            alert('Please enter at least one client entry');
            return;
        }

        setLoading(true);
        try {
            const addedApps = [];
            for (const line of lines) {
                // simple CSV parsing handling optional commas in earning
                const parts = line.split(',').map(p => p.trim());
                if (parts.length >= 4) {
                    const clientName = parts[0];
                    const platform = parts[1];
                    const deviceCount = parseInt(parts[2], 10) || 0;

                    // Re-join anything past the 3rd column as earning, strip non-digits
                    const earningRaw = parts.slice(3).join('');
                    const earning = parseInt(earningRaw.replace(/\D/g, ''), 10) || 0;

                    await createMasterApp({
                        clientName,
                        platform,
                        deviceCount,
                        earning,
                        status: 'DRAFT'
                    });
                    addedApps.push(clientName);
                } else {
                    console.warn(`Line skipped (invalid format): ${line}`);
                }
            }

            if (addedApps.length === 0) {
                alert('No valid lines found. Format MUST be: ClientName,Platform,Devices,Earning');
            } else {
                alert(`Successfully added ${addedApps.length} clients to Master List as Drafts.`);
                router.push('/');
            }

        } catch (err) {
            console.error(err);
            alert('Failed to create master apps');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-3xl mx-auto px-4 py-8">
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Add Bulk Master Drafts</h1>
                    <p className="text-gray-500 mt-1">Quickly stage new clients before gathering technical app links.</p>
                </div>
                <Link href="/" className="text-blue-600 hover:underline">Cancel</Link>
            </div>

            <form onSubmit={handleSubmit} className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 space-y-4">
                <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg mb-6 border border-blue-100 dark:border-blue-800">
                    <h3 className="text-sm font-semibold text-blue-800 dark:text-blue-300 mb-2">Required Format (CSV):</h3>
                    <code className="text-xs block bg-white dark:bg-gray-900 p-2 rounded text-gray-800 dark:text-gray-200">
                        ClientName, Platform, TotalDevices, Earning
                    </code>
                    <p className="text-xs text-blue-600 dark:text-blue-400 mt-2">Example:</p>
                    <code className="text-xs block bg-white dark:bg-gray-900 p-2 rounded text-gray-800 dark:text-gray-200">
                        Budi Store, Shopee, 5, 50000<br />
                        Andi Jaya, Tokopedia, 10, 150000<br />
                        Siska Online, Direct, 1, 10000
                    </code>
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Paste Data Here (One entry per line)
                    </label>
                    <textarea
                        value={bulkInput}
                        onChange={(e) => setBulkInput(e.target.value)}
                        placeholder="Budi Store, Shopee, 5, 50000"
                        className="w-full border rounded-lg px-4 py-3 dark:bg-gray-700 dark:border-gray-600 dark:text-white h-64 font-mono text-sm shadow-inner"
                        required
                    />
                </div>

                <div className="flex gap-4 pt-4">
                    <button
                        type="submit"
                        disabled={loading}
                        className="flex-1 bg-blue-600 text-white font-medium py-3 rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
                    >
                        {loading ? 'Processing Data...' : 'Save All as DRAFT'}
                    </button>

                </div>
            </form>
        </div>
    );
}
