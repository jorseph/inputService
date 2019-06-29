//-----------------------------------------------------------------------------
//  Copyright (c) 2017 Qualcomm Technologies, Inc.
//  All Rights Reserved. Qualcomm Technologies Proprietary and Confidential.
//-----------------------------------------------------------------------------

#include <jni.h>
#include "RingBuffer.h"
#include <pthread.h>
#include <android/log.h>
#include "svrApi.h"
#include <string.h>

#define JNI_FUNC(x) Java_io_xrspace_inputs_ControllerContext_##x
struct timespec t;
class NativeContext {
    public:
        svrControllerState state;
        RingBuffer<svrControllerState>* sharedMemory;
    public:
        NativeContext()
        {
            sharedMemory = 0;
			state.rotation.x = 0;
			state.rotation.y = 0;
			state.rotation.z = 0;
			state.rotation.w = 1;
			
			state.position.x = 0;
			state.position.y = 0;
			state.position.z = 0;
			
			state.accelerometer.x = 0;
			state.accelerometer.y = 0;
			state.accelerometer.z = 0;
			
			state.gyroscope.x = 0;
			state.gyroscope.y = 0;
			state.gyroscope.z = 0;
			
		
			state.connectionState = (svrControllerConnectionState)0;
			state.buttonState = 0;
			state.isTouching = 0;
			state.timestamp = 0;
			for(int i=0;i<4;i++)
			{
				state.analog2D[i].x = 0;
				state.analog2D[i].y = 0;
			}
			
			for(int i=0;i<8;i++)
			{
				state.analog1D[i] = 0;
			}

            //state.gesture.position_L.x = 0;
            //state.gesture.position_L.y = 0;
            //state.gesture.position_L.z = 0;
//
            //state.gesture.position_R.x = 0;
            //state.gesture.position_R.y = 0;
            //state.gesture.position_R.z = 0;
//
            //state.gesture.rotation_L.x = 0;
            //state.gesture.rotation_L.y = 0;
            //state.gesture.rotation_L.z = 0;
            //state.gesture.rotation_R.x = 0;
            //state.gesture.rotation_R.y = 0;
            //state.gesture.rotation_R.z = 0;
//
			//state.gesture.gestureID_L = 0;
            //state.gesture.gestureID_R = 0;
//
            //state.deviceType = (svrDeviceType)0;
        }

        ~NativeContext()
        {
            delete sharedMemory;
            sharedMemory = 0;
        }
};

//-----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL JNI_FUNC(updateNativeConnectionState)(JNIEnv *jniEnv, jobject, jint ptr, jint state)
//-----------------------------------------------------------------------------
{
	NativeContext* entry = (NativeContext*)(ptr);
	entry->state.connectionState = (svrControllerConnectionState)state;

    if( entry->sharedMemory != 0 )
    {
        entry->sharedMemory->set(&entry->state);
    }
}

//-----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL JNI_FUNC(updateNativeStateGesture)(JNIEnv *jniEnv, jobject, jint ptr, jint state, jint btns,
                                                                     jfloat pos0, jfloat pos1, jfloat pos2,
                                                                     jfloat rot0, jfloat rot1, jfloat rot2, jfloat rot3,
                                                                     jfloat gyro0, jfloat gyro1, jfloat gyro2,
                                                                     jfloat acc0, jfloat acc1, jfloat acc2,
                                                                     jint timestamp, jint touchpads, jfloat x, jfloat y,
                                                                     jint gestureID, jint devicetype, jint body0, jint body1, jint body2, jint index)
//-----------------------------------------------------------------------------
{
    //LOGI("the state size = %d", sizeof(svrControllerState));
    NativeContext *entry = (NativeContext *) (ptr);

    entry->state.rotation.x = rot0;
    entry->state.rotation.y = rot1;
    entry->state.rotation.z = -rot2;
    entry->state.rotation.w = -rot3;

    entry->state.position.x = pos0;
    entry->state.position.y = pos1;
    entry->state.position.z = pos2;

    entry->state.accelerometer.x = acc0;
    entry->state.accelerometer.y = acc1;
    entry->state.accelerometer.z = acc2;

    entry->state.gyroscope.x = gyro0;
    entry->state.gyroscope.y = gyro1;
    entry->state.gyroscope.z = gyro2;

    entry->state.connectionState = (svrControllerConnectionState) state;
    entry->state.buttonState = btns;
    entry->state.isTouching = touchpads;
    entry->state.timestamp = timestamp;
    entry->state.analog2D[0].x = x;
    entry->state.analog2D[0].y = y;

    entry->state.bodys[0] = body0;
    entry->state.bodys[1] = body1;
    entry->state.bodys[2] = body2;

    entry->state.gesture = gestureID;
    entry->state.index = index;

    //LOGI("pos_gesture = %f, %f, %f", pos0 , pos1, pos2);
    //LOGI("rotation is = %f, %f, %f, %f", rot0 , rot1, rot2, rot3);

    entry->state.deviceType = devicetype;
    if(devicetype == 6 || devicetype == 7) {
        struct timeval tv;
        gettimeofday(&tv, NULL);
        uint64_t t = tv.tv_sec;
        t *=1000;
        t +=tv.tv_usec/1000;
        LOGI("dFrameInfo:%llu:%d:Ctrl_Svc_Out:%d,%f,%f,%f,%f,%f,%f,%f,%d", t, index, devicetype, pos0, pos1, pos2, rot0, rot1, rot2, rot3, gestureID);
    }

    if (entry->sharedMemory != 0) {
        entry->sharedMemory->set(&entry->state);
    }
}

//-----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL JNI_FUNC(updateNativeState)(JNIEnv *jniEnv, jobject, jint ptr, jint state, jint btns,
                                                                                                    jfloat pos0, jfloat pos1, jfloat pos2,
                                                                                                    jfloat rot0, jfloat rot1, jfloat rot2, jfloat rot3,
                                                                                                    jfloat gyro0, jfloat gyro1, jfloat gyro2,
                                                                                                    jfloat acc0, jfloat acc1, jfloat acc2,
                                                                                                    jint timestamp, jint touchpads, jfloat x, jfloat y,
                                                                                                    jint devicetype, jfloat body0)
//-----------------------------------------------------------------------------
{
    NativeContext* entry = (NativeContext*)(ptr);

    entry->state.rotation.x = rot0;
    entry->state.rotation.y = rot1;
    entry->state.rotation.z = -rot2;
    entry->state.rotation.w = -rot3;

    entry->state.position.x = pos0;
    entry->state.position.y = pos1;
    entry->state.position.z = pos2;
	
	entry->state.accelerometer.x = acc0;
	entry->state.accelerometer.y = acc1;
	entry->state.accelerometer.z = acc2;
	
	entry->state.gyroscope.x = gyro0;
	entry->state.gyroscope.y = gyro1;
	entry->state.gyroscope.z = gyro2;
	

    entry->state.connectionState = (svrControllerConnectionState)state;
    entry->state.buttonState = btns;
    entry->state.isTouching = touchpads;
    entry->state.timestamp = timestamp;
    entry->state.analog2D[0].x = x;
    entry->state.analog2D[0].y = y;

    entry->state.deviceType = devicetype;
    //entry->state.bodys.x = body0;
    //entry->state.bodys.y = 0;
    //entry->state.bodys.z = 0;
    //LOGI("rotation = %f, %f, %f, %f ", rot0 , rot1, -rot2, -rot3);
    if( entry->sharedMemory != 0 )
    {
        entry->sharedMemory->set(&entry->state);
    }

    //__android_log_print(ANDROID_LOG_ERROR, "XXXXX", "state updated - %f %f %f", p0, p1, p2);
}

//-----------------------------------------------------------------------------
extern "C" JNIEXPORT jint JNICALL JNI_FUNC(createNativeContext)(JNIEnv *jniEnv, jobject, jint fd, jint size)
//-----------------------------------------------------------------------------
{
    NativeContext* nativeContext = new NativeContext();
    nativeContext->sharedMemory = RingBuffer<svrControllerState>::fromFd(fd, size, false);
    return (jint)(nativeContext);
}

//-----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL JNI_FUNC(freeNativeContext)(JNIEnv *jniEnv, jobject, jint ptr)
//-----------------------------------------------------------------------------
{
    NativeContext* nativeContext = (NativeContext*)(ptr);
    if( nativeContext != 0 )
    {
        delete nativeContext;
    }
}
