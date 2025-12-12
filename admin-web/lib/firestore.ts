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
