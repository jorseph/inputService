//
// Created by 許森宥 on 2019/3/4.
//
#include <jni.h>
#include <pthread.h>
#include <android/log.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <errno.h>
#include <sched.h>
#include <cstdio>
#include <controller/SvrControllerUtil/inc/RingBuffer.h>

#define JNI_FUNC(x) Java_io_xrspace_inputs_XRSpaceInputService_##x
void set_cur_thread_affinity(int mask) {
    int err, syscallres;
    pid_t pid = gettid();
    LOGI("mask is %x", mask&0xFF);
    syscallres = syscall(__NR_sched_setaffinity, pid, sizeof(mask), &mask);
    if (syscallres) {
        err = errno;
        LOGE("Error in the syscall setaffinity: mask = %d, err=%d", mask, errno);
    }

    LOGI("tid = %d has setted affinity success",pid);
}

int get_cur_thread_affinity(cpu_set_t get) {
    int err, syscallres;
    pid_t pid = gettid();
    syscallres = syscall(__NR_sched_getaffinity, pid, sizeof(get), &get);
    if (syscallres < 0) {
        err = syscallres;
        //LOGI("Error in the syscall getaffinity: get = %x, err=%d", get, err);
        return err;

    }

    LOGI("tid = %d has getted affinity success",pid);
    return 1;
}

static int getCores() {
    return sysconf(_SC_NPROCESSORS_CONF);
}

extern "C" JNIEXPORT int JNICALL JNI_FUNC(getCores)(JNIEnv *env, jobject) {
    return getCores();
}

extern "C" JNIEXPORT void JNICALL JNI_FUNC(bindToCpu)(JNIEnv *env, jobject, jint cpu) {
    int cores = getCores();
    LOGI("get cpu number = %d\n",cores);
    if (cpu >= cores) {
        LOGE("your set cpu is beyond the cores,exit...");
        return;
    }

    cpu_set_t mask;
    //cpu_set_t get;
    CPU_ZERO(&mask);
    LOGI("ZERO mask = %x",((int)&mask)&0xFF);
    CPU_SET(0,&mask);
    CPU_SET(1,&mask);
    CPU_SET(2,&mask);
    CPU_SET(3,&mask);
    CPU_SET(4,&mask);
    CPU_SET(5,&mask);
    CPU_SET(6,&mask);
    CPU_SET(7,&mask);
    LOGI("CPU_SET mask = %x",((int)&mask)&0xFF);
    //set_cur_thread_affinity((int)(&mask));
    sched_setaffinity(0,sizeof(mask), &mask);

    struct sched_param param;
    printf("Thread %d sched_setscheduler()\n", gettid());
    param.sched_priority = 48;
    sched_setscheduler(0, SCHED_FIFO, &param);
    printf("Thread %d sched after %s\n", gettid(), strerror(errno));


    //LOGI("set affinity to %d success",cpu);

    //CPU_ZERO(&get);
    //if (get_cur_thread_affinity(get) < 0) {
    //    LOGE("get thread affinity failed\n");
    //}
    //for (int j = 0; j < cores; j++) {
    //    if (CPU_ISSET(j, &get)) {
    //        LOGI("thread %d is running in processor %d\n", gettid(), j);
    //    }
    //}
}