PACKAGE_NAME=com.firebirdberlin.tinytimetracker

debug:
	ant debug

release:
	ant release

installdebug:
	adb $(OPT) install -r ./build/outputs/apk/debug/android-debug.apk

install:
	adb $(OPT) install -r release/android-release.apk

uninstall:
	adb $(OPT) uninstall ${PACKAGE_NAME}

clean:
	ant clean

start:
	adb $(OPT) shell am start -n ${PACKAGE_NAME}/${PACKAGE_NAME}.TinyTimeTracker

on:
	adb shell input keyevent KEYCODE_POWER

clear-data:
	adb shell pm clear ${PACKAGE_NAME}
