ifeq ($(origin ANDROID_HOME),undefined)
    $(error ANDROID_HOME is not set. Please set this environment variable to your Android SDK path.)
endif

ADB = ${ANDROID_HOME}/platform-tools/adb
PACKAGE_NAME=com.firebirdberlin.tinytimetracker

debug:
	gradle assembleDebug

release:
	gradle assembleRelease

installdebug:
	${ADB} $(OPT) install -r ./build/outputs/apk/debug/app-debug.apk

install:
	${ADB} $(OPT) install -r ./build/outputs/apk/release/app-release.apk

uninstall:
	${ADB} $(OPT) uninstall ${PACKAGE_NAME}

clean:
	gradle clean

start:
	${ADB} $(OPT) shell am start -n ${PACKAGE_NAME}/${PACKAGE_NAME}.TinyTimeTracker

on:
	${ADB} shell input keyevent KEYCODE_POWER

clear-data:
	${ADB} shell pm clear ${PACKAGE_NAME}

doze-on:
	${ADB} $(OPT) shell dumpsys deviceidle force-idle

doze-off:
	${ADB} $(OPT) shell dumpsys deviceidle unforce