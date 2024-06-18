#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "path_resolver.h"

int main(int argc, char const *argv[])
{
    printf("===== Performing Tests! =====\n");
    printf("Creating pathresolver!\n");
    struct PathResolver *pr = pathresolver_create();
    
    assert(pr != NULL);
    assert(pr->entries != NULL);
    assert(pr->size == 0);
    assert(pr->capacity == 2);
    printf("Creation successful!\n");

    printf("Adding entries!\n");
    int rval = add_path(pr, 1, "path1");

    assert(rval == 0);
    assert(pr->size == 1);
    assert(pr->capacity == 2);
    assert(pr->entries[0].watch_descriptor == 1);
    assert(strcmp(pr->entries[0].path, "path1") == 0);

    rval = add_path(pr, 2, "path2");
    assert(rval == 0);

    rval = add_path(pr, 3, "path3");
    assert(rval == 0);

    assert(pr->size == 3);
    assert(pr->capacity == 4);
    assert(pr->entries[0].watch_descriptor == 1);
    assert(strcmp(pr->entries[0].path, "path1") == 0);
    assert(pr->entries[1].watch_descriptor == 2);
    assert(strcmp(pr->entries[1].path, "path2") == 0);
    assert(pr->entries[2].watch_descriptor == 3);
    assert(strcmp(pr->entries[2].path, "path3") == 0);
    printf("Adding successful!\n");

    printf("Getting entries!\n");
    struct PathResolverEntry *existing_entry = get_path(pr, 1);

    assert(existing_entry != NULL);
    assert(existing_entry->watch_descriptor == 1);
    assert(strcmp(existing_entry->path, "path1") == 0);

    struct PathResolverEntry *nonexisting_entry = get_path(pr, 42);

    assert(nonexisting_entry == NULL);
    printf("Getting successful!\n");

    printf("Destroying pathresolver!\n");
    pathresolver_destroy(pr);
    printf("Destruction successful - Tested with valgrind!\n");

    printf("===== All tests passed! =====\n");
    return EXIT_SUCCESS;
}
