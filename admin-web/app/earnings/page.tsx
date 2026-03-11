'use client';

import { useState, useEffect } from 'react';
import { getMasterApps, type MasterApp } from '@/lib/firestore';

type TimeFilter = 'TODAY' | 'THIS_MONTH' | 'THIS_YEAR' | 'ALL_TIME';

export default function EarningsDashboard() {
    const [apps, setApps] = useState<MasterApp[]>([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState<TimeFilter>('THIS_MONTH');

    useEffect(() => {
        const loadApps = async () => {
            setLoading(true);
            try {
                // Fetch all apps directly, filtering logic happens client side 
                // for flexibility since amounts might be small enough initially
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

    const getFilteredEarnings = () => {
        const now = new Date();
        const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
        const startOfYear = new Date(now.getFullYear(), 0, 1).getTime();

        let filteredApps = apps;

        if (filter === 'TODAY') {
            filteredApps = apps.filter(app => app.createdAt >= startOfDay);
        } else if (filter === 'THIS_MONTH') {
            filteredApps = apps.filter(app => app.createdAt >= startOfMonth);
        } else if (filter === 'THIS_YEAR') {
            filteredApps = apps.filter(app => app.createdAt >= startOfYear);
        }

        const total = filteredApps.reduce((sum, app) => sum + (app.earning || 0), 0);
        return {
            total,
            count: filteredApps.length
        };
    };

    const { total, count } = getFilteredEarnings();

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-4xl mx-auto space-y-8">

                <div className="text-center">
                    <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white sm:text-4xl">
                        Earnings Dashboard
                    </h1>
                    <p className="mt-3 text-xl text-gray-500 sm:mt-4">
                        Aggregate financial overview across all registered master clients.
                    </p>
                </div>

                {/* Filters */}
                <div className="flex justify-center space-x-2">
                    {(['TODAY', 'THIS_MONTH', 'THIS_YEAR', 'ALL_TIME'] as const).map((f) => (
                        <button
                            key={f}
                            onClick={() => setFilter(f)}
                            className={`px-6 py-2 rounded-full text-sm font-bold transition-shadow ${filter === f
                                ? 'bg-green-600 text-white shadow-lg ring-2 ring-green-600 ring-offset-2 dark:ring-offset-gray-900'
                                : 'bg-white text-gray-600 border dark:bg-gray-800 dark:text-gray-300 dark:border-gray-700 hover:bg-gray-50'
                                }`}
                        >
                            {f.replace('_', ' ')}
                        </button>
                    ))}
                </div>

                {/* Main Metric - GEDE HAPPY */}
                <div className="bg-gradient-to-br from-green-500 to-emerald-700 rounded-3xl shadow-xl overflow-hidden relative">
                    <div className="absolute top-0 right-0 -mt-4 -mr-4 w-32 h-32 bg-white opacity-10 rounded-full blur-2xl"></div>
                    <div className="absolute bottom-0 left-0 -mb-4 -ml-4 w-40 h-40 bg-black opacity-10 rounded-full blur-2xl"></div>

                    <div className="px-6 py-16 sm:p-20 text-center relative z-10">
                        <p className="text-green-100 font-semibold uppercase tracking-wider mb-2">Total Project Value</p>
                        {loading ? (
                            <div className="h-20 w-64 bg-green-400/30 animate-pulse rounded-lg mx-auto"></div>
                        ) : (
                            <div className="text-5xl sm:text-7xl font-extrabold text-white tracking-tight break-all">
                                Rp {total.toLocaleString('id-ID')}
                            </div>
                        )}
                        <p className="mt-4 text-green-100/80 font-medium">From {count} registered client apps</p>
                    </div>
                </div>

                {/* Earnings Split */}
                {!loading && total > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-8">
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-blue-500">
                            <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Bos Nur (1/3)</p>
                            <p className="mt-2 text-2xl font-bold gap-text-gray-900 dark:text-white text-blue-600">
                                Rp {Math.floor(total / 3).toLocaleString('id-ID')}
                            </p>
                        </div>
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-pink-500">
                            <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Cici Linda (1/3)</p>
                            <p className="mt-2 text-2xl font-bold gap-text-gray-900 dark:text-white text-pink-600">
                                Rp {Math.floor(total / 3).toLocaleString('id-ID')}
                            </p>
                        </div>
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-purple-500">
                            <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Cici Zulfa (1/3)</p>
                            <p className="mt-2 text-2xl font-bold gap-text-gray-900 dark:text-white text-purple-600">
                                Rp {Math.floor(total / 3).toLocaleString('id-ID')}
                            </p>
                        </div>
                    </div>
                )}

                {loading && <p className="text-center text-gray-500 animate-pulse">Calculating earnings...</p>}

            </div>
        </div>
    );
}
