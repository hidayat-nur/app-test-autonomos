'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { getTasksByDate, deleteTask, groupTasksByType, type DailyTask, type TaskType } from '@/lib/firestore';

function getTodayDate(): string {
  const today = new Date();
  return today.toISOString().split('T')[0];
}

const TASK_TYPE_LABELS: Record<TaskType, string> = {
  DELETE_APP: 'Hapus App Baru',
  RATE_APP: 'Rating App',
  TEST_APP: 'Test App Baru',
  UPDATE_APP: 'Update App',
};

const TASK_TYPE_COLORS: Record<TaskType, string> = {
  DELETE_APP: 'bg-red-100 border-red-300',
  RATE_APP: 'bg-yellow-100 border-yellow-300',
  TEST_APP: 'bg-blue-100 border-blue-300',
  UPDATE_APP: 'bg-green-100 border-green-300',
};

export default function Dashboard() {
  const [date, setDate] = useState(getTodayDate());
  const [tasks, setTasks] = useState<DailyTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadTasks = async () => {
    console.log('loadTasks called for date:', date);
    setLoading(true);
    setError(null);
    try {
      const data = await getTasksByDate(date);
      console.log('Tasks loaded:', data);
      setTasks(data);
    } catch (err) {
      console.error('Error loading tasks:', err);
      setError(err instanceof Error ? err.message : 'Failed to load tasks');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    console.log('useEffect triggered');
    loadTasks();
  }, [date]);

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this task?')) return;
    try {
      await deleteTask(id);
      setTasks(tasks.filter(t => t.id !== id));
    } catch (err) {
      alert('Failed to delete task');
    }
  };

  const grouped = groupTasksByType(tasks);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Header */}
      <header className="bg-white dark:bg-gray-800 shadow">
        <div className="max-w-7xl mx-auto px-4 py-6 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Daily Task Admin
          </h1>
          <Link
            href="/tasks/new"
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
          >
            + Add Task
          </Link>
        </div>
      </header>

      {/* Date Picker */}
      <div className="max-w-7xl mx-auto px-4 py-4">
        <div className="flex items-center gap-4">
          <label className="font-medium text-gray-700 dark:text-gray-300">Date:</label>
          <input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="border rounded-lg px-3 py-2 dark:bg-gray-800 dark:border-gray-700 dark:text-white"
          />
          <button
            onClick={() => setDate(getTodayDate())}
            className="text-blue-600 hover:underline"
          >
            Today
          </button>
        </div>
      </div>

      {/* Content */}
      <main className="max-w-7xl mx-auto px-4 py-6">
        {loading && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="border rounded-lg p-4 bg-gray-100 animate-pulse h-32"></div>
            ))}
          </div>
        )}
        {error && <p className="text-red-500">Error: {error}</p>}

        {!loading && !error && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {(Object.keys(TASK_TYPE_LABELS) as TaskType[]).map((type) => (
              <TaskSection
                key={type}
                type={type}
                label={TASK_TYPE_LABELS[type]}
                tasks={grouped[type]}
                colorClass={TASK_TYPE_COLORS[type]}
                onDelete={handleDelete}
              />
            ))}
          </div>
        )}

        {!loading && !error && tasks.length === 0 && (
          <p className="text-center text-gray-500 py-8">
            No tasks for this date. <Link href="/tasks/new" className="text-blue-600 hover:underline">Add one?</Link>
          </p>
        )}
      </main>
    </div>
  );
}

function TaskSection({
  type,
  label,
  tasks,
  colorClass,
  onDelete,
}: {
  type: TaskType;
  label: string;
  tasks: DailyTask[];
  colorClass: string;
  onDelete: (id: string) => void;
}) {
  return (
    <div className={`border rounded-lg p-4 ${colorClass}`}>
      <h2 className="text-lg font-semibold mb-3">{label}</h2>
      {tasks.length === 0 ? (
        <p className="text-gray-500 text-sm">No tasks</p>
      ) : (
        <ul className="space-y-2">
          {tasks.map((task) => (
            <li
              key={task.id}
              className="bg-white dark:bg-gray-800 p-3 rounded shadow-sm flex items-center justify-between"
            >
              <div>
                <p className="font-medium">{task.appName}</p>
                <p className="text-sm text-gray-500">{task.packageName}</p>
              </div>
              <div className="flex gap-2">
                <Link
                  href={`/tasks/${task.id}/edit`}
                  className="text-blue-600 hover:underline text-sm"
                >
                  Edit
                </Link>
                <button
                  onClick={() => onDelete(task.id!)}
                  className="text-red-600 hover:underline text-sm"
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
