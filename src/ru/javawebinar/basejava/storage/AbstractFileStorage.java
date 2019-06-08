package ru.javawebinar.basejava.storage;

import ru.javawebinar.basejava.exception.StorageException;
import ru.javawebinar.basejava.model.Resume;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractFileStorage extends AbstractStorage<File> {

    private final File directory;

    public AbstractFileStorage(File directory) {
        Objects.requireNonNull(directory, "directory is required");
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory.getName() + " is not directory.");
        }
        if (!directory.canRead() || !directory.canWrite()) {
            throw new StorageException("directory is not available for RW operations.", directory.getName());
        }
        this.directory = directory;
    }

    @Override
    protected File findKey(String uuid) {
        return new File(directory, uuid);
    }

    @Override
    protected boolean isExistKey(File file) {
        return file.exists();
    }

    @Override
    protected Resume doGet(File file) {
        try {
            return doRead(new BufferedInputStream(new FileInputStream(file)));

        } catch (IOException e) {
            throw new StorageException("Couldn't read the resume", file.getName(), e);
        }
    }

    @Override
    protected void doUpdate(File file, Resume resume) {
        try {
            doWrite(new BufferedOutputStream(new FileOutputStream(file)), resume);
        } catch (IOException e) {
            throw new StorageException("Couldn't update the resume", file.getName(), e);
        }
    }

    @Override
    protected void doDelete(File file) {
        if (!file.delete()) {
            throw new StorageException("Couldn't delete the file", file.getName());
        }
    }

    @Override
    protected void doSave(File file, Resume resume) {
        try {
            if (file.createNewFile()) {
                doWrite(new BufferedOutputStream(new FileOutputStream(file)), resume);
            } else {
                throw new StorageException("Couldn't save the file", file.getName());
            }
        } catch (IOException e) {
            throw new StorageException("Couldn't create the new resume.", file.getName(), e);
        }
    }

    @Override
    protected List<Resume> copyStorage() {
        final File[] files = directory.listFiles();
        List<Resume> resumes = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                try {
                    resumes.add(doRead(new BufferedInputStream(new FileInputStream(file))));
                } catch (IOException e) {
                    throw new StorageException("Couldn't read the resume.", file.getName(), e);
                }
            }
        }
        return resumes;
    }

    @Override
    public int size() {
        return Objects.requireNonNull(directory.listFiles()).length;
    }

    @Override
    public void clear() {
        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                doDelete(file);
            }
        }
    }

    protected abstract void doWrite(OutputStream outputStream, Resume resume) throws IOException;

    protected abstract Resume doRead(InputStream inputStream) throws IOException;
}
