'use client';

import { useState, useEffect } from 'react';
import { getMasterApps, type MasterApp } from '@/lib/firestore';

type QuickFilter = 'TODAY' | 'THIS_MONTH' | 'THIS_YEAR' | 'ALL_TIME' | 'CUSTOM';

const MONTHS = [
    'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
    'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember',
];

export default function EarningsDashboard() {
    const [apps, setApps] = useState<MasterApp[]>([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState<QuickFilter>('THIS_MONTH');

    const now = new Date();
    const [pickerMonth, setPickerMonth] = useState(now.getMonth()); // 0-indexed
    const [pickerYear, setPickerYear] = useState(now.getFullYear());

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
        // ALL_TIME
        return apps;
    };

    const filtered = getFilteredApps();
    const total = filtered.reduce((sum, a) => sum + (a.earning || 0), 0);
    const count = filtered.length;

    // Build year options: from 2024 to current year
    const yearOptions: number[] = [];
    for (let y = 2024; y <= now.getFullYear(); y++) yearOptions.push(y);

    const filterLabel = filter === 'CUSTOM'
        ? `${MONTHS[pickerMonth]} ${pickerYear}`
        : filter.replace(/_/g, ' ');

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

                {/* Quick filters */}
                <div className="flex justify-center flex-wrap gap-2">
                    {(['TODAY', 'THIS_MONTH', 'THIS_YEAR', 'ALL_TIME'] as const).map((f) => (
                        <button
                            key={f}
                            onClick={() => setFilter(f)}
                            className={`px-6 py-2 rounded-full text-sm font-bold transition-shadow ${filter === f
                                ? 'bg-green-600 text-white shadow-lg ring-2 ring-green-600 ring-offset-2 dark:ring-offset-gray-900'
                                : 'bg-white text-gray-600 border dark:bg-gray-800 dark:text-gray-300 dark:border-gray-700 hover:bg-gray-50'
                                }`}
                        >
                            {f.replace(/_/g, ' ')}
                        </button>
                    ))}
                </div>

                {/* Month / Year picker */}
                <div className={`flex justify-center items-center gap-2 p-3 rounded-2xl border-2 transition ${filter === 'CUSTOM' ? 'border-green-500 bg-green-50 dark:bg-green-900/20' : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800'}`}>
                    <span className="text-sm font-semibold text-gray-500 dark:text-gray-400 mr-1">📅 Pilih Bulan:</span>
                    <select
                        value={pickerMonth}
                        onChange={e => { setPickerMonth(Number(e.target.value)); setFilter('CUSTOM'); }}
                        className="px-3 py-1.5 rounded-lg border text-sm font-medium bg-white dark:bg-gray-700 dark:text-white dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-green-500"
                    >
                        {MONTHS.map((m, i) => (
                            <option key={i} value={i}>{m}</option>
                        ))}
                    </select>
                    <select
                        value={pickerYear}
                        onChange={e => { setPickerYear(Number(e.target.value)); setFilter('CUSTOM'); }}
                        className="px-3 py-1.5 rounded-lg border text-sm font-medium bg-white dark:bg-gray-700 dark:text-white dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-green-500"
                    >
                        {yearOptions.map(y => (
                            <option key={y} value={y}>{y}</option>
                        ))}
                    </select>
                    {filter !== 'CUSTOM' && (
                        <button
                            onClick={() => setFilter('CUSTOM')}
                            className="px-4 py-1.5 rounded-lg bg-green-600 text-white text-sm font-bold hover:bg-green-700 transition"
                        >
                            Lihat
                        </button>
                    )}
                </div>

                {/* Main Metric */}
                <div className="bg-gradient-to-br from-green-500 to-emerald-700 rounded-3xl shadow-xl overflow-hidden relative">
                    <div className="absolute top-0 right-0 -mt-4 -mr-4 w-32 h-32 bg-white opacity-10 rounded-full blur-2xl"></div>
                    <div className="absolute bottom-0 left-0 -mb-4 -ml-4 w-40 h-40 bg-black opacity-10 rounded-full blur-2xl"></div>

                    <div className="px-6 py-16 sm:p-20 text-center relative z-10">
                        <p className="text-green-100 font-semibold uppercase tracking-wider mb-1">Total Project Value</p>
                        <p className="text-green-200/70 text-sm mb-4">{filterLabel}</p>
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
                            <p className="mt-2 text-2xl font-bold text-blue-600">
                                Rp {Math.floor(total / 3).toLocaleString('id-ID')}
                            </p>
                        </div>
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-pink-500">
                            <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Cici Linda (1/3)</p>
                            <p className="mt-2 text-2xl font-bold text-pink-600">
                                Rp {Math.floor(total / 3).toLocaleString('id-ID')}
                            </p>
                        </div>
                        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 text-center border-t-4 border-purple-500">
                            <p className="text-gray-500 dark:text-gray-400 text-sm font-medium uppercase tracking-wide">Cici Zulfa (1/3)</p>
                            <p className="mt-2 text-2xl font-bold text-purple-600">
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
