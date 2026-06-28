#include <jni.h>
#include <string>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/select.h>

extern "C" JNIEXPORT jboolean JNICALL
Java_sa_studio_port7caner_MainActivity_checkPort(JNIEnv* env, jobject thiz, jstring ip_str, jint port) {

    const char* ip = env->GetStringUTFChars(ip_str, nullptr);

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        env->ReleaseStringUTFChars(ip_str, ip);
        return JNI_FALSE;
    }

    int flags = fcntl(sock, F_GETFL, 0);
    fcntl(sock, F_SETFL, flags | O_NONBLOCK);

    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);

    if (inet_pton(AF_INET, ip, &addr.sin_addr) <= 0) {
        close(sock);
        env->ReleaseStringUTFChars(ip_str, ip);
        return JNI_FALSE;
    }

    int res = connect(sock, (struct sockaddr*)&addr, sizeof(addr));

    if (res < 0) {
        if (errno == EINPROGRESS) {
            struct timeval tv;
            tv.tv_sec = 0;
            tv.tv_usec = 200000;

            fd_set fdset;
            FD_ZERO(&fdset);
            FD_SET(sock, &fdset);

            if (select(sock + 1, nullptr, &fdset, nullptr, &tv) > 0) {
                int so_error;
                socklen_t len = sizeof(so_error);
                getsockopt(sock, SOL_SOCKET, SO_ERROR, &so_error, &len);
                if (so_error == 0) {
                    close(sock);
                    env->ReleaseStringUTFChars(ip_str, ip);
                    return JNI_TRUE;
                }
            }
        }
    } else {
        close(sock);
        env->ReleaseStringUTFChars(ip_str, ip);
        return JNI_TRUE;
    }

    close(sock);
    env->ReleaseStringUTFChars(ip_str, ip);
    return JNI_FALSE;
}