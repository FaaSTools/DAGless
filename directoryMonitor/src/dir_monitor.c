#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/inotify.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <limits.h>
#include <dirent.h>
#include <stdbool.h>
#include "path_resolver.h"

#define EVENT_SIZE (sizeof(struct inotify_event))
#define BUF_LEN (1024 * (EVENT_SIZE + 16))

// Global log file
const char* access_log_file_path;
const char* modify_log_file_path;
// Global path resolver
struct PathResolver *pr;

// Function to recursively watch a directory
void watch_directory(int inotify_fd, const char* path);
// Function to write to file conditionally
void write_to_file_conditionally(int fd, const char* path);
// Function to check if path is a file
int is_file(const char* path);
// Function to check if path is a directory
int is_dir(const char* path);

int main(int argc, char* argv[]) {
    // Check if directory to monitor was passed as argument
    if (argc != 4) {
        fprintf(stderr, "Usage: %s <directory_to_monitor> <path_to_access_logfile> <path_to_modify_logfile>\n", argv[0]);
        return 1;
    }

    // Create inotify instance
    int inotify_fd = inotify_init();
    if (inotify_fd == -1) {
        perror("inotify_init");
        return 1;
    }

    // Create path resolver instance
    pr = pathresolver_create();
    if (pr == NULL) {
        perror("pathresolver_create");
        return 1;
    }

    // Get directory to monitor from arguments
    const char* directory_to_monitor = argv[1];
    // Get log file path from arguments
    access_log_file_path = argv[2];
    modify_log_file_path = argv[3];

    // Recursively watch directory and subdirectories
    watch_directory(inotify_fd, directory_to_monitor);

    // Create log file
    int access_fd = open(access_log_file_path, O_WRONLY | O_APPEND | O_CREAT, S_IRUSR | S_IWUSR);
    if (access_fd == -1) {
        perror("open");
        return 1;
    }

    int modify_fd = open(modify_log_file_path, O_WRONLY | O_APPEND | O_CREAT, S_IRUSR | S_IWUSR);
    if (modify_fd == -1) {
        perror("open");
        return 1;
    }

    // Create buffer for inotify events
    char buffer[BUF_LEN];
    ssize_t num_read;

    // Main loop to read inotify events
    while (1) {
        num_read = read(inotify_fd, buffer, BUF_LEN);
        if (num_read <= 0) {
            perror("read");
            return 1;
        }

        // Iterate over all events
        struct inotify_event* event;
        for (char* ptr = buffer; ptr < buffer + num_read; ptr += sizeof(struct inotify_event) + event->len) {
            event = (struct inotify_event*)ptr;
            // Get PathResolverEntry for watch descriptor
            struct PathResolverEntry *entry = get_path(pr, event->wd);
             // Create full path variable to store the resolved relative path of the file/dir
            char full_path[PATH_MAX];
            // Check if file was accessed, modified or created
            if (event->mask & IN_ACCESS){ // ACTIVATE HERE IS NECESSARY || event->mask & IN_MODIFY || event->mask & IN_CREATE) {
                if (entry == NULL) {
                    // If entry is NULL, only the file name is stored in the event
                    snprintf(full_path, PATH_MAX, "%s", event->name);
                    write_to_file_conditionally(access_fd, full_path);
                } else {
                    // If the event is a create we need to check if the created file is a directory
                    /* ACTIVATE IF YOU WANT TO WATCH NEWLY CREATED DIRECTORIES
                    if (event->mask & IN_CREATE){
                        snprintf(full_path, PATH_MAX, "%s/%s", entry->path, event->name);
                        if (is_dir(full_path)) {
                            // add new directory to watch list
                            watch_directory(inotify_fd, full_path);
                        }
                    }
                    */
                    // Resolve relative path of file and write to log file
                    snprintf(full_path, PATH_MAX, "%s/%s", entry->path, event->name);
                    write_to_file_conditionally(access_fd, full_path);
                }
            }
            if (event->mask & IN_MODIFY){
                if (entry == NULL) {
                    // If entry is NULL, only the file name is stored in the event
                    snprintf(full_path, PATH_MAX, "%s", event->name);
                    write_to_file_conditionally(modify_fd, full_path);
                } else {
                    // If the event is a create we need to check if the created file is a directory
                    /* ACTIVATE IF YOU WANT TO WATCH NEWLY CREATED DIRECTORIES
                    if (event->mask & IN_CREATE){
                        snprintf(full_path, PATH_MAX, "%s/%s", entry->path, event->name);
                        if (is_dir(full_path)) {
                            // add new directory to watch list
                            watch_directory(inotify_fd, full_path);
                        }
                    }
                    */
                    // Resolve relative path of file and write to log file
                    snprintf(full_path, PATH_MAX, "%s/%s", entry->path, event->name);
                    write_to_file_conditionally(modify_fd, full_path);
                }
            }
        }
    }

    close(inotify_fd);
    close(access_fd);
    close(modify_fd);

    return 0;
}

void write_to_file_conditionally(int fd, const char* path) {
   if (is_file(path)) {
        dprintf(fd, "%s\n", path);
   }
}

int is_file(const char* path) {
    struct stat statbuf;
    if (stat(path, &statbuf) == 0 && S_ISREG(statbuf.st_mode)) {
        return true;
    }
    return false;
}

int is_dir(const char* path){
    struct stat statbuf;
    if (stat(path, &statbuf) == 0 && S_ISDIR(statbuf.st_mode)) {
        return true;
    }
    return false;
}

void watch_directory(int inotify_fd, const char* path) {
    int wd = inotify_add_watch(inotify_fd, path, IN_ACCESS | IN_ONLYDIR | IN_CREATE | IN_MODIFY);
    if (wd == -1) {
        
        perror("inotify_add_watch");
        return;
    }

    // Add path to path resolver
    int r = add_path(pr, wd, path);
    if (r != 0) {
        perror("add_path failed");
        return;
    }

    printf("Watching directory: %s with wd: %d\n", path, wd);

    DIR* dir = opendir(path);
    if (dir == NULL) {
        perror("opendir");
        return;
    }

    struct dirent* entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strcmp(entry->d_name, ".") != 0 && strcmp(entry->d_name, "..") != 0) {
            char full_path[PATH_MAX];
            snprintf(full_path, PATH_MAX, "%s/%s", path, entry->d_name);

            struct stat statbuf;
            if (stat(full_path, &statbuf) == 0 && S_ISDIR(statbuf.st_mode)) {
                watch_directory(inotify_fd, full_path);
            }
        }
    }
    closedir(dir);
}
