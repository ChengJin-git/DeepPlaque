/* Export component and composite .tif from .xlof files within .xlef project file, acquired by Leica microscopes via BioFormats importer 
 *  then save as hyperstack (component data with separate channels) and composite image (RGB merged channels) 
 */

macro "BioFormats import Leica images from within .xlef project file" {

// Define which image source to be imported into BioFormats
var export = true;
var imageSource = "WSI";
var microscope = "THUNDER";
var LeicaProjectRegex = ".*Ab_ThS_40X.*\\.lif";
//var LeicaProjectRegex = ".*Ab_ThS_SytoxGreen_40X.*\\.lif";
var seriesIndexStart = 11;
var wsi_imageNum = 12;
var nChannel = 3;

// Define what export formats are needed
var hyperstack_component = true
var hyperstack_color = false
var composite_RGB = true
var extractAbetaChannel = true
	// channel no. for DAPI and Abeta immunostains							
	var DAPI_ch = 2; 	// exported composite image's suffix = _ch01.tif
	var Abeta_ch = 1; 	// exported composite image's suffix = _ch00.tif

	// Define whether and what extent to adjust brightness & contrast of image stack 
	var adjustAbetaHistogram = false
	var saturationFactor = 0.005;
	// Define whether to export montage 
	var montage = false
	var montage_ch = "original";
	var markerPanel = "ThS+Abeta+DAPI";

	// (0) Import widefield IF image from Leica project .lif file via BioFormats 
	var input = "/path/to/input/folder";  // TODO: Set your input folder path
	var outputFolder = "/path/to/output/folder";  // TODO: Set your output folder path
	var lifprojectSuffix = "40X"
	var outputDir = outputFolder + "/" + markerPanel + "_component_40X_THUNDER";
	dir = File.getParent(input);
	IJ.log("processing directory: " + input);

	    var list = getFileList(input);
	    list = Array.sort(list);
	    
	    	/* function: append data rows to array */
			function append(arr, value) {
			    arr2 = newArray(arr.length+1);
			    for (a=0; a<arr.length; a++)
			        arr2[a] = arr[a];
			        arr2[arr.length] = value;
			    return arr2;
			}
			
		var project = newArray(0);
		var imagelist = newArray(0);
		var WSIlist = newArray(0);
		var croplist = newArray(0);
		
			var imageRegex = ".*\\.lof";
			var cropRegex = ".*Crop00.*\\.lof";
			
			// extract file subset by matching with the defined regex pattern (suffix/extension)]
			    for (j=0; j<list.length; j++) {
			        if (matches(list[j], imageRegex)) {
			            imagelist = append(imagelist, list[j]);
			            Array.sort(imagelist);
			        } 
				        if (matches(list[j], cropRegex)) {
				            croplist = append(croplist, list[j]);
				            Array.sort(croplist);
				        } else if (matches(list[j], LeicaProjectRegex)) {
				        	project = append(project, list[j]);
			        	} else if (matches(list[j], imageRegex)) {
				        	WSIlist = append(WSIlist, list[j]);
				        	Array.sort(WSIlist);
			        	}
		        }
		        
		    if (microscope == "MICA") {
				imageNum_to_process = imagelist.length;
	    		print("n of .lof files = " + imagelist.length + "; n of WSI = "+ WSIlist.length + "; n of crops = "+ croplist.length);
    		
			} else if (microscope == "THUNDER") {
				imageNum_to_process = wsi_imageNum;
			}
    		
    	// (1) Import WSI crops series by series within .xlef project file by 
    	var projectFile = project[0];
    	print(projectFile);
    	print(projectFile.length);
    	
		var lefproject = input + "/" + projectFile;
		
			if (imageSource == "crops") {
				seriesIndexStart = WSIlist.length+1;
			} 
			
		for (seriesNum = seriesIndexStart ; seriesNum < imageNum_to_process+1 ; seriesNum++) {

			showProgress(seriesNum+1, imageNum_to_process);
			print("processing series #" + seriesNum);
			
			// Open the virtual image stack stored within .xlef file
				run("Bio-Formats Importer", "open=["+lefproject+"] color_mode=Colorized open_files view=[Data Browser] stack_order=XYCZT use_virtual_stack series_" + seriesNum);
				var imageWindow = getTitle();
				
					if (microscope == "MICA") {
						var Fileindex = indexOf(imageWindow, lifprojectSuffix + ".xlef");
						var imageIDindexStart = Fileindex + lifprojectSuffix.length + 8;
					} else if (microscope == "THUNDER") {
						var Fileindex = indexOf(imageWindow, lifprojectSuffix + ".lif");
						print(Fileindex);
						var imageIDindexStart = Fileindex + lifprojectSuffix.length + 7;
					}
				
				var imageID = substring(imageWindow, imageIDindexStart, imageWindow.length);
				print(imageID);

						print("-------------------Imported " + imageID + "-------------------");

					if (imageID == "Sample Overview") {
						selectImage(imageWindow); 
						close(); 
						continue;
					}
				
				
				if (microscope == "MICA") {
					
					var imageFile = imageID + ".lof";
		
					// skip image exports if it does not match with the required image source type	
					if (croplist.length>0){
						for (crop=0; crop<croplist.length; crop++) {
							if (matches(croplist[crop], imageFile)) {
								if (imageSource == "crops"){export = true;}
								else if (imageSource == "WSI"){export = false;}
									// Exit loop on match
		      						break;
							} else {
								if (imageSource == "crops"){export = false;}
								else if (imageSource == "WSI"){export = true;}
							}
						}
						print("export or not: " + imageID + " = " + export);
					}
				}
			
			if (export == true){
						
				print("continue with image exports of " + imageSource + " ( #" + (seriesNum) + " among " + imageNum_to_process + " images )");
				
					selectImage(imageWindow);
					// Edit image stack properties before export 
					// run("Properties...", "channels=3 slices=1 frames=1 pixel_width=0.2875007 pixel_height=0.2875012 voxel_depth=1.0000000");
					
						// duplicate stack to adjust pseudocolours
						run("Make Subset...", "channels=1-"+nChannel);
						var colourWindow = getTitle();
						// Edit LUT by channel using "Channels Tool"
						selectImage(colourWindow);
						run("Channels Tool...");
						Property.set("CompositeProjection", "null");
						Stack.setDisplayMode("color");
							Stack.setChannel(DAPI_ch);
							run("Blue");
							Stack.setChannel(Abeta_ch);
							run("Yellow");
							// run("Brightness/Contrast...");
							// resetMinAndMax();
						
						// Convert stack to hyperstack before export 
						run("Stack to Hyperstack...", "order=xyczt(default) channels="+ nChannel +" slices=1 frames=1 display=Color");
						rename(imageID + "-component_YB.tif");
						
					if (hyperstack_color == true){
						if (adjustAbetaHistogram == true) {
							run("Enhance Contrast", "saturated=" + saturationFactor);
							saveAs("Tiff", outputDir + "/" + imageID + "-component_YB-" + saturationFactor + ".tif");
							print("exported: component image with adjusted pseudocolour LUT in tuned histogram channel.");
						} else {
							resetMinAndMax();
							saveAs("Tiff", outputDir + "/" + imageID + "-component_YB.tif");
							print("exported: component image with adjusted pseudocolour LUT in original histogram channel.");
						}
					}
						
					// export composite RGB overlay image 
					if (composite_RGB == true) {
						selectImage(imageID + "-component_YB.tif");
						run("Make Composite");
						if (adjustAbetaHistogram == true) {
							Stack.setChannel(3);
							run("Enhance Contrast", "saturated=" + saturationFactor);
							Stack.setActiveChannels("101");
							run("Stack to RGB");
							saveAs("Tiff", outputDir + "/" + imageID + "-composite_YB-" + saturationFactor + ".tif");
							print("exported: composite RGB image with YB channels in tuned histogram.");
						} else {
							resetMinAndMax();
							Stack.setActiveChannels("101");
							run("Stack to RGB");
							saveAs("Tiff", outputDir + "/" + imageID + "-composite_YB.tif");
							print("exported: composite RGB image with YB channels in original histogram.");
						}
					}
					
					if (extractAbetaChannel == true) {
						selectImage(imageID + "-component_YB.tif");
						run("Make Composite");
						Stack.setActiveChannels("001");
						resetMinAndMax();
						run("Stack to RGB");
						saveAs("Tiff", outputDir + "/" + imageID + "-composite_abeta.tif");
						print("exported: abeta channel in RGB.");
					}
	
					if (hyperstack_component == true) {
						// export component image data with separate raw channels in hyperstack  
						selectImage(imageWindow);
						resetMinAndMax();
						run("Stack to Hyperstack...", "order=xyczt(default) channels="+nChannel +" slices=1 frames=1 display=Color");
						saveAs("Tiff", outputDir + "/" + imageID + "-component_ORG.tif");
						print("exported: component image with original pseudocolour LUT in original histogram channel.");
					}
					
					// make montage to display channels side by side
					if (montage == true) {
						
						if (montage_ch == "original"){selectImage(imageWindow); var output = outputDir + "/" + imageID + "-channels_montage_ORG.tif";
						} else if (montage_ch == "adjusted") {selectImage(imageID + "-component_YB.tif"); var output = outputDir + "/" + imageID + "-channels_montage_" + saturationFactor + ".tif";}
						
						Stack.getDimensions(width, height, channels, slices, frames);
						run("Make Subset...", "channels=1-"+channels);
							var subsetWindow = getTitle();
							selectImage(subsetWindow);
							
						if (channels >3) { 
							var nRows = channels/2;
							run("Make Montage...", "columns="+ channels +" rows="+ nRows +" scale=0.25 label");
						} else {run("Make Montage...", "columns="+ channels +" rows=1 scale=0.25 label");}
		
						saveAs("Tiff", output);
						print("exported: " + channels + " channels' montage");
					}
				
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				// waitForUser("check?");
				run("Close All");
				print("processed " + imageID);
			
		} else {
			print("skipped "+imageID);
		}
	}
		selectWindow("Log");
		timeNow = call("java.time.Instant.now");
		timeNowString = substring(timeNow,0,16);
		timeNowString = replace(timeNowString,"T", "_");
		timeNowString = replace(timeNowString,":", "");
		saveAs("Text", dir + "/Log_" + timeNowString + "_" + markerPanel + "_stampPatches.txt");		
	waitForUser("Done.");
}
