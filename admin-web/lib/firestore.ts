import { initializeApp, getApps } from 'firebase/app';
import { getFirestore, collection, doc, getDocs, getDoc, addDoc, updateDoc, deleteDoc, query, where, orderBy } from 'firebase/firestore';
import firebaseConfig from './firebase-config';

// Initialize Firebase
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];
const db = getFirestore(app);

// Types
export type TaskType = 'DELETE_APP' | 'RATE_APP' | 'TEST_APP' | 'UPDATE_APP' | 'NOTES';

export interface DailyTask {
    id?: string;
    date: string;
    appName: string;
    packageName: string;
    taskType: TaskType;
    playStoreUrl: string;
    acceptUrl: string;
    createdAt: number;
}

const COLLECTION_NAME = 'daily_tasks';
const MASTER_APPS_COLLECTION = 'master_apps';

export type MasterAppStatus = 'DRAFT' | 'PUBLISHED' | 'DELETED';

// Expanded MasterApp Schema V2
export interface MasterApp {
    id?: string;
    clientName: string;
    platform: string;
    deviceCount: number;
    earning: number;
    credentials?: string;
    appName?: string;
    packageName?: string;
    playStoreUrl?: string;
    acceptUrl?: string;
    rateDate?: string;
    deleteDate?: string;
    status: MasterAppStatus;
    createdAt: number;
    updatedAt: number;
}


// Get all tasks for a specific date
export async function getTasksByDate(date: string): Promise<DailyTask[]> {
    console.log('Fetching tasks for date:', date);

    // Add timeout to detect slow connections
    const timeoutPromise = new Promise<never>((_, reject) => {
        setTimeout(() => reject(new Error('Firestore request timeout (5s) - check security rules')), 5000);
    });

    try {
        const q = query(
            collection(db, COLLECTION_NAME),
            where('date', '==', date)
        );

        const snapshot = await Promise.race([
            getDocs(q),
            timeoutPromise
        ]);

        console.log('Found', snapshot.docs.length, 'tasks');
        const tasks = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as DailyTask));
        return tasks.sort((a, b) => b.createdAt - a.createdAt);
    } catch (error) {
        console.error('Firestore error:', error);
        throw error;
    }
}

// Get all tasks (with optional date filter)
export async function getAllTasks(date?: string): Promise<DailyTask[]> {
    let q;
    if (date) {
        q = query(collection(db, COLLECTION_NAME), where('date', '==', date), orderBy('createdAt', 'desc'));
    } else {
        q = query(collection(db, COLLECTION_NAME), orderBy('date', 'desc'), orderBy('createdAt', 'desc'));
    }
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as DailyTask));
}

// Get single task by ID
export async function getTaskById(id: string): Promise<DailyTask | null> {
    const docRef = doc(db, COLLECTION_NAME, id);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        return { id: docSnap.id, ...docSnap.data() } as DailyTask;
    }
    return null;
}

// Create new task
export async function createTask(task: Omit<DailyTask, 'id' | 'createdAt'>): Promise<string> {
    const docRef = await addDoc(collection(db, COLLECTION_NAME), {
        ...task,
        createdAt: Date.now()
    });
    return docRef.id;
}

// Update task
export async function updateTask(id: string, task: Partial<DailyTask>): Promise<void> {
    const docRef = doc(db, COLLECTION_NAME, id);
    await updateDoc(docRef, task);
}

// Delete task
export async function deleteTask(id: string): Promise<void> {
    const docRef = doc(db, COLLECTION_NAME, id);
    await deleteDoc(docRef);
}

// Group tasks by type
export function groupTasksByType(tasks: DailyTask[]): Record<TaskType, DailyTask[]> {
    return {
        DELETE_APP: tasks.filter(t => t.taskType === 'DELETE_APP'),
        RATE_APP: tasks.filter(t => t.taskType === 'RATE_APP'),
        TEST_APP: tasks.filter(t => t.taskType === 'TEST_APP'),
        UPDATE_APP: tasks.filter(t => t.taskType === 'UPDATE_APP'),
        NOTES: tasks.filter(t => t.taskType === 'NOTES'),
    };
}


// --- Master Apps CRUD ---

export async function getMasterApps(status?: MasterAppStatus): Promise<MasterApp[]> {
    let q;
    if (status) {
        q = query(collection(db, MASTER_APPS_COLLECTION), where('status', '==', status), orderBy('updatedAt', 'desc'));
    } else {
        q = query(collection(db, MASTER_APPS_COLLECTION), orderBy('updatedAt', 'desc'));
    }

    const timeoutPromise = new Promise<never>((_, reject) => {
        setTimeout(() => reject(new Error('Firestore request timeout (5s)')), 5000);
    });

    try {
        const snapshot = await Promise.race([
            getDocs(q),
            timeoutPromise
        ]);
        return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as MasterApp));
    } catch (error) {
        console.error('Firestore getMasterApps error:', error);
        throw error;
    }
}

export async function createMasterApp(app: Omit<MasterApp, 'id' | 'createdAt' | 'updatedAt'>): Promise<string> {
    const now = Date.now();
    const docRef = await addDoc(collection(db, MASTER_APPS_COLLECTION), {
        ...app,
        createdAt: now,
        updatedAt: now,
    });
    return docRef.id;
}

export async function updateMasterApp(id: string, appData: Partial<MasterApp>): Promise<void> {
    const docRef = doc(db, MASTER_APPS_COLLECTION, id);
    await updateDoc(docRef, { ...appData, updatedAt: Date.now() });
}

export async function getMasterAppById(id: string): Promise<MasterApp | null> {
    const docRef = doc(db, MASTER_APPS_COLLECTION, id);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        return { id: docSnap.id, ...docSnap.data() } as MasterApp;
    }
    return null;
}

export async function deleteMasterAppAndTasks(id: string, packageName?: string): Promise<void> {
    // 1. Delete Master Record
    const docRef = doc(db, MASTER_APPS_COLLECTION, id);
    await deleteDoc(docRef);

    // 2. Cascade delete tasks by packageName if provided
    if (packageName) {
        const q = query(
            collection(db, COLLECTION_NAME),
            where('packageName', '==', packageName)
        );
        const snapshot = await getDocs(q);
        const deletePromises = snapshot.docs.map(doc => deleteDoc(doc.ref));
        await Promise.all(deletePromises);
        console.log(`Cascade deleted ${deletePromises.length} tasks for package ${packageName}`);
    }
}

// --- Data Migration ---
export async function migrateLegacyData(): Promise<{ migrated: number, skipped: number }> {
    const NINETY_DAYS_MS = 90 * 24 * 60 * 60 * 1000;
    const cutoffTime = Date.now() - NINETY_DAYS_MS;

    console.log('Starting Migration. Fetching legacy tasks...');

    // Fetch daily tasks created in the last 90 days. We assume date or createdAt could be used. 
    // We'll just fetch all and filter client side for safety since we might not have a composite index.
    const allTasks = await getAllTasks();
    const recentTasks = allTasks.filter(t => t.createdAt >= cutoffTime && t.taskType !== 'NOTES');

    console.log(`Found ${recentTasks.length} non-note tasks in the last 90 days.`);

    // Deduplicate by package name
    const uniquePackages = new Map<string, DailyTask>();
    for (const task of recentTasks) {
        if (task.packageName && !uniquePackages.has(task.packageName)) {
            uniquePackages.set(task.packageName, task);
        }
    }

    console.log(`Found ${uniquePackages.size} unique packages.`);

    // Fetch existing Master Apps to avoid duplicates
    const existingMasterApps = await getMasterApps();
    const existingPackages = new Set(existingMasterApps.map(app => app.packageName).filter(Boolean));

    let migratedCount = 0;
    let skippedCount = 0;

    for (const [packageName, task] of uniquePackages.entries()) {
        if (existingPackages.has(packageName)) {
            skippedCount++;
            continue;
        }

        // Migrate to MasterApp format
        await createMasterApp({
            clientName: 'Legacy Client', // Default
            platform: 'Unknown',        // Default
            deviceCount: 0,             // Default
            earning: 0,                 // Default
            appName: task.appName,
            packageName: task.packageName,
            playStoreUrl: task.playStoreUrl,
            acceptUrl: task.acceptUrl,
            status: 'PUBLISHED'         // Legacy tasks are considered active/published
        });
        migratedCount++;
    }

    console.log(`Migration complete. Migrated: ${migratedCount}, Skipped: ${skippedCount}`);
    return { migrated: migratedCount, skipped: skippedCount };
}
