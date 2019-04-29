package ru.javawebinar.basejava.storage;

import ru.javawebinar.basejava.model.Resume;

import java.util.Arrays;

public abstract class AbstractArrayStorage implements Storage {

    protected int currentSize;
    protected final int MAX_CAPACITY = 10_000;
    protected final Resume[] STORAGE = new Resume[MAX_CAPACITY];

    public int size() {
        return currentSize;
    }

    public Resume[] getAll() {
        return Arrays.copyOf(STORAGE, currentSize);
    }

    public void clear() {
        Arrays.fill(STORAGE, 0, currentSize, null);
        currentSize = 0;
    }

    public void save(Resume resume) {
        if (currentSize == MAX_CAPACITY) {
            System.out.println("Resume hasn't been added. Maximum storage size reached.");
            return;
        }

        int index = findIndex(resume.getUuid());
        if (index >= 0) {
            System.out.println("Resume hasn't been added. Resume with such uuid exists.");
            return;
        }
        insert(index, resume);
        currentSize++;
    }

    public void delete(String uuid) {
        int index = findIndex(uuid);
        if (index >= 0) {
            remove(index);
            STORAGE[currentSize - 1] = null;
            currentSize--;
        } else {
            System.out.println("Resume hasn't been found.");
        }
    }

    public void update(Resume resume) {
        int index = findIndex(resume.getUuid());
        if (index >= 0) {
            STORAGE[index] = resume;
        } else {
            System.out.println("Resume hasn't been found.");
        }
    }

    public Resume get(String uuid) {
        int index = findIndex(uuid);
        if (index >= 0) {
            return STORAGE[index];
        } else {
            System.out.println("Resume hasn't been found.");
            return null;
        }
    }

    protected abstract void remove(int i);

    protected abstract void insert(int index, Resume resume);

    protected abstract int findIndex(String uuid);

}
