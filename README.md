# DocDeskew Utility                  

## DESCRIPTION
DocDeskew is a perspective correction tool for photographed documents.
It features a Dual Engine Architecture: attempting to use OpenCV for
advanced edge detection and homography, but safely falling back to a
Pure Java Bilinear Warp if OpenCV is not installed on your system.

## ENABLING OPENCV (OPTIONAL BUT RECOMMENDED)
To use the advanced homography and auto-detection features, install OpenCV:
* **Windows:** Download from opencv.org, extract, and add `build/java/x64` to your System PATH.
* **macOS:** Run `brew install opencv` in your terminal.
* **Linux (Mint/Ubuntu):** Run `sudo apt install libopencv-java` in your terminal.

## HOW TO RUN
* **Windows:** Double-click `run.bat`
* **macOS:** Double-click `run_mac.command`
* **Linux:** Run `./run.sh` in your terminal

> **Note:** You must have Java installed on your computer to run this application.

## USAGE
1. Upload an image via **File -> Open** or **Edit -> Paste**.
2. The app will attempt to auto-detect the document corners.
3. Drag the red corner points to fine-tune the boundaries using the loupe.
4. Click **'Deskew Now'** to process the image.
5. Save or copy your result from the new tab.
