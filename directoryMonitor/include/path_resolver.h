#ifndef PATHRESOLVER_H
#define PATHRESOLVER_H

#define MAX_PATH_LENGTH 1024

struct PathResolverEntry
{
    int watch_descriptor;
    char path[MAX_PATH_LENGTH];
};

struct PathResolver
{
    struct PathResolverEntry *entries;
    int size;
    int capacity;
};

struct PathResolver *pathresolver_create();

int add_path(struct PathResolver *resolver, int watch_descriptor, const char *path);

struct PathResolverEntry *get_path(struct PathResolver *resolver, int watch_descriptor);

void pathresolver_destroy(struct PathResolver *resolver);


#endif //PATHRESOLVER_H


