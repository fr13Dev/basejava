package ru.javawebinar.basejava.storage;

import ru.javawebinar.basejava.model.Resume;

import java.util.Arrays;
import java.util.Comparator;

public class SortedArrayStorage extends AbstractArrayStorage {

    private final Comparator<Resume> comparator = Comparator.comparing(Resume::getUuid);

    @Override
    protected void remove(int index) {
        System.arraycopy(storage, index + 1, storage, index, currentSize - index - 1);
    }

    @Override
    protected void insert(int index, Resume resume) {
        if (currentSize == 0) {
            storage[currentSize] = resume;
        } else {
            int currentIndex = -index - 1;
            System.arraycopy(storage, currentIndex, storage, currentIndex + 1, currentSize - currentIndex);
            storage[currentIndex] = resume;
        }
    }

    @Override
    protected Integer findKey(String uuid) {
        Resume r = new Resume(uuid, "dummy");
        return Arrays.binarySearch(storage, 0, currentSize, r, comparator);
    }
}
