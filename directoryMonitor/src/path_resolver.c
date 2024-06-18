#include "path_resolver.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct PathResolver *pathresolver_create() {
    struct PathResolver *pr = malloc(sizeof(struct PathResolver));

    if (pr == NULL) {
        perror("malloc failed at creating pathresolver struct");
        exit(1);
    }

    pr->size = 0;
    pr->capacity = 2;
    pr->entries = (struct PathResolverEntry*) malloc(sizeof(struct PathResolverEntry) * pr->capacity);

    if (pr->entries == NULL) {
        perror("malloc failed at creating pathresolver entries");
        exit(1);
    }
    return pr;
}

int add_path(struct PathResolver *resolver, int watch_descriptor, const char *path) {
    if (resolver->size == resolver->capacity) {
        resolver->capacity *= 2;
        resolver->entries = realloc(resolver->entries, sizeof(struct PathResolverEntry) * resolver->capacity);

        if (resolver->entries == NULL) {
            perror("realloc failed");
            exit(1);
        }
    }

    struct PathResolverEntry *entry = &resolver->entries[resolver->size];
    entry->watch_descriptor = watch_descriptor;

    if (strlen(path) > MAX_PATH_LENGTH) {
        printf("Path too long!\n");
        return 1;
    }
    strcpy(entry->path, path);

    resolver->size++;
    return 0;
}


struct PathResolverEntry *get_path(struct PathResolver *resolver, int watch_descriptor) {
    for (int i = 0; i < resolver->size; i++) {
        struct PathResolverEntry *entry = &resolver->entries[i];
        if (entry->watch_descriptor == watch_descriptor) {
            return entry;
        }
    }
    return NULL;
}


void pathresolver_destroy(struct PathResolver *pr) {
    if (pr->entries != NULL){
        free(pr->entries);
    }
    free(pr);
}


