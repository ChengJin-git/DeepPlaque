/* Export component and composite .tif from .czi image files acquired by ZEISS microscopes via BioFormats importer 
 *  then save as hyperstack (component data with separate channels) and composite image (RGB merged channels) 
 */	
	
	// Define what export formats are needed
	var montage = true
	
	// Import widefield IF image from .czi file via BioFormats 
	var dir = "/path/to/input/folder";  // TODO: Set your input folder path
	var inputDir = dir + "/02_ZEISS_czi_raw";
	var outputDir = dir + "/02_ZEISS_czi_BioFormats/" + "01_WSI";
	
	var imageID = "885";
	var cropID = "01";
	var extension = ".czi";
	var filename = dir + File.separator + imageID + File.separator + cropID + extension
	
	// Open as virtual image stackt to save memory 
	run("Bio-Formats Importer", "open=["+filename+"] color_mode=Colorized open_files view=[Data Browser] stack_order=XYCZT use_virtual_stack");
	
	
macro "BioFormats import Zeiss .czi files" {
	
// Define whether and what extent to adjust brightness & contrast of image stack 
var adjustHistogram = true
var saturationFactor = 0.05;

// Define what export formats are needed
var hyperstack_component = true
var hyperstack_color = true
var composite_RGB = true
var extractAbetaChannel = true
var montage = true
var montage_ch = "adjusted";

	// (0) Import widefield IF image from Leica project .lif file via BioFormats 
	var input = "/path/to/input/folder";  // TODO: Set your input folder path
	var outputDir = "/path/to/output/folder";  // TODO: Set your output folder path
	dir = File.getParent(input);
	IJ.log("processing directory: " + input);

		var imagelist = newArray(0);
		var croplist = newArray(0);
		var project = newArray(0);
		
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
			
			// extract file subset by matching with the defined regex pattern (suffix/extension)]
			var wsiRegex = ".*\\.lof";
			var cropRegex = ".*Crop00.*\\.lof";
				
			    for (j=0; j<list.length; j++) {
			        if (matches(list[j], wsiRegex)) {
			            imagelist = append(imagelist, list[j]);
			        } 
			        if (matches(list[j], cropRegex)) {
			            croplist = append(croplist, list[j]);
			        } else if (endsWith(list[j], ".xlef")) {
			        	project = append(project, list[j]);
		        	}	
		        }
		        
    		print("imagelist = " + imagelist.length + "; croplist = "+ croplist.length);
        	
    	// (1) Import WSI crops series by series within .xlef project file by 
    	var projectFile = project[0];
		var lefproject = input + "/" + projectFile;
		
		for (seriesNum=0; seriesNum<imagelist.length; seriesNum++) {

			showProgress(seriesNum+1, imagelist.length);
			print("processing series #" + seriesNum);
			
				var imageFile = imagelist[seriesNum];
				imageFile = toString(imageFile); 
				var imageID = substring(imageFile, 0,(imageFile.length-4));
				print("image file = " + imageID);
	
				// skip image exports if it does not match with the required image source type	
//				if (croplist.length>0){
//					for (crop=0; crop<croplist.length; crop++) {
//						if (matches(croplist[crop], imageFile)) {
//							if (imageSource == "crops"){export = true;}
//							else if (imageSource == "WSI"){export = false;}
//						} else {
//							if (imageSource == "crops"){export = false;}
//							else if (imageSource == "WSI"){export = true;}
//						}
//					}
//					print("export or not: " + imageID + " = " + export);
//				}
//			
			print("continue with image exports of " + imageFile + " ( #" + (seriesNum+1) + " among " + imagelist.length + " images )");
			
			// Open the virtual image stack stored within .xlef file
			run("Bio-Formats Importer", "open=["+lefproject+"] color_mode=Colorized open_files view=[Data Browser] stack_order=XYCZT use_virtual_stack series_" + seriesNum);
			var imageWindow = getTitle();
				
			if (imageID == "Sample Overview") {
				selectImage(imageWindow); 
				close(); 
				continue;
			} else {
					
				selectImage(imageWindow);
				// Edit image stack properties before export 
				run("Properties...", "channels=3 slices=1 frames=1 pixel_width=0.2875007 pixel_height=0.2875012 voxel_depth=1.0000000");
				
					// duplicate stack to adjust pseudocolours
					run("Make Subset...", "channels=1-3");
					var colourWindow = getTitle();
					// Edit LUT by channel using "Channels Tool"
					selectImage(colourWindow);
					run("Channels Tool...");
					Property.set("CompositeProjection", "null");
					Stack.setDisplayMode("color");
						Stack.setChannel(1);
						run("Blue");
						Stack.setChannel(3);
						run("Yellow");
						run("Brightness/Contrast...");
						resetMinAndMax();
						if (adjustHistogram == true) {run("Enhance Contrast", "saturated=" + saturationFactor);}
					
					// Convert stack to hyperstack before export 
					run("Stack to Hyperstack...", "order=xyczt(default) channels=3 slices=1 frames=1 display=Color");
					rename(imageID + "-component_YB.tif");
					
				if (hyperstack_color == true){
					saveAs("Tiff", outputDir + "/" + imageID + "-component_YB.tif");
					print("exported: component image with adjusted channel pseudocolour LUT.");
				}
					
				// export composite RGB overlay image 
				if (composite_RGB == true) {
					selectImage(imageID + "-component_YB.tif");
					run("Make Composite");
					Stack.setActiveChannels("101");
					run("Stack to RGB");
					saveAs("Tiff", outputDir + "/" + imageID + "-composite_YB.tif");
					print("exported: composite RGB image as overlay channels.");
				}
				
				if (extractAbetaChannel == true) {
					selectImage(imageID + "-component_YB.tif");
					run("Make Composite");
					Stack.setActiveChannels("001");
					run("Stack to RGB");
					saveAs("Tiff", outputDir + "/" + imageID + "-composite_abeta.tif");
					print("exported: abeta channel in RGB.");
				}

				if (hyperstack_component == true) {
					// export component image data with separate raw channels in hyperstack  
					selectImage(imageWindow);
					run("Stack to Hyperstack...", "order=xyczt(default) channels=3 slices=1 frames=1 display=Color");
					saveAs("Tiff", outputDir + "/" + imageID + "-component_ORG.tif");
					print("exported: original component image in separate channels.");
				}
				
				// make montage to display channels side by side
				if (montage == true) {
					
					if (montage_ch == "original"){selectImage(imageWindow);} else if (montage_ch == "adjusted") {selectImage(imageID + "-component_YB.tif");}
					
					Stack.getDimensions(width, height, channels, slices, frames);
					run("Make Subset...", "channels=1-"+channels);
						var subsetWindow = getTitle();
						selectImage(subsetWindow);
						
					if (channels >3) { 
						var nRows = channels/2;
						run("Make Montage...", "columns="+ channels +" rows="+ nRows +" scale=0.25 label");
					} else {run("Make Montage...", "columns="+ channels +" rows=1 scale=0.25 label");}
	
					saveAs("Tiff", outputDir + "/" + imageID + "-channels_montage.tif");
					print("exported: " + channels + " channels' montage");
				}
				
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// waitForUser("check?");
			run("Close All");
			print("processed " + imageID);
		
		}
	}
			
	waitForUser("Done.");
}