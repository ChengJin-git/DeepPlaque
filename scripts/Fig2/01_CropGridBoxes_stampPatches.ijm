/* Contextual patch stamping within large WSI crops, to yield maximum grids with the same size and resolution as reference stamp
 * input = WSI crops' composite + component .tif images  
 * output = Abeta+ patches (composite + component) with calibrated image size equivalent to Akoya 1x1 tile stamp    
 */
 
macro "patch stamping within WSI crops .tif file, with optional random sampling" {
	
/**********************************************************************************************/
	
	var WSIfolder = "/path/to/WSI/folder";  // TODO: Set your WSI folder path
	var refcrop_image = "PhenoImager_template_composite.tif";
	var refcrop_imagePath = WSIfolder + "/" + refcrop_image;
	dir = File.getParent(WSIfolder);
	
	var markerPanel = "ThS+Abeta+DAPI";
	var compositeFolder = WSIfolder + "/" + markerPanel + "_composite_40X_THUNDER";
	var componentFolder = WSIfolder + "/" + markerPanel + "_component_40X_THUNDER";

	var abetaPatchOnly = false
	var abetaAreaThreshold = 15000;  // pixel threshold for filter before export of patch
	
		/* spatial calibration:  
		 *  reference crop's resolution = 3.9954 px/µm (0.2503 µm/px)
		 *  WSI-based image's resolution = 3.665 px/µm (0.2729 µm/px)
		 */
 		// resolution of each image input (px/μm)
		var wsiRes = 3.665;
		var refRes = 3.9953602268;
		
		// Determine the resolution ratios between the WSI and reference images
		// Ratio = Reference / WSI = 3.9954 / 3.665 = 1.0894
		var recalibrationRatio = refRes / wsiRes;
		print("The conversion ratio for recalibration = " + recalibrationRatio);
	
	// Define the target size of rectangular ROI as stamp
	var cropXYstart = "remainXY";
	var exportPatches = true;
	var channelsTOComponent = false;
	var saveComponent = true;
	var saveComposite = true;
	var compositeImg = "Abeta+DAPI";
	
	var randomSelectPatches = false;

		if (randomSelectPatches == true) {
			// enable pop up window for user-defined no. of grid boxes to randomly select
		    // var grids = getNumber("How many grid boxes to select?", "");
		    // this is limited to the maximum no. of patches within each crop... 
		    var grids = 20; 
		}
	
	// to run test on image subset, define one ID in regex to filter only respective images 
	var testID = "ThS+Abeta+DAPI";
		if (testID != ""){
			var testIDregexPrefix = ".*"+ testID;
		} else {
			var testIDregexPrefix = "";
		}
	
	var fileSuffix_component = testIDregexPrefix + ".*component_ORG\\.tif";
	var fileSuffix_overlay = testIDregexPrefix + ".*overlay\\.tif";
	var fileSuffix_abeta = testIDregexPrefix + ".*ch00_SV\\.tif";
	
/**********************************************************************************************/

		// Define the arrays to store filenames matched with defined regex suffix  
		var stackList = newArray(0);
		var imagelist = newArray(0);
		var ch00list = newArray(0);
		
	    var compositeList = getFileList(compositeFolder);
	    compositeList = Array.sort(compositeList);
	    var componentList = getFileList(componentFolder);
	    componentList = Array.sort(componentList);
	    
	    	/* function: append data rows to array */
			function append(arr, value) {
			    arr2 = newArray(arr.length+1);
			    for (a=0; a<arr.length; a++)
			        arr2[a] = arr[a];
			        arr2[arr.length] = value;
			    return arr2;
			}
		
		// Append images with matched suffix to the component stack list  
        for (i=0; i<componentList.length; i++) {
	        if (matches(componentList[i], fileSuffix_component)) {
	            stackList = append(stackList, componentList[i]);
	            Array.sort(stackList);
	        }
        }
        
		// Append images with matched suffix to respective composite channel/overlay list 
	    for (j=0; j<compositeList.length; j++) {
	        if (matches(compositeList[j], fileSuffix_overlay)) {
	            imagelist = append(imagelist, compositeList[j]);
	            Array.sort(imagelist);
	        } else if (matches(compositeList[j], fileSuffix_abeta)) {
	        	ch00list = append(ch00list, compositeList[j]);
	        	Array.sort(ch00list);
        	} 	
        }

    IJ.log("processing " + imagelist.length + " composite images, among " + stackList.length + " component images");

/**********************************************************************************************/

	// loop through each Abeta+DAPI composite image, find matching component image for further processing  
    for (img=0; img<imagelist.length; img++) {

		showProgress(img+1, imagelist.length);
		
		// open the reference crop image for spatial calibration 
		open(refcrop_imagePath);
		
		var filename = imagelist[img];

			// extract ID and size of the source image (cropped WSI) 
			var cropIndex = indexOf(filename, "_overlay");
			var startIndex = indexOf(filename, "40X_");
			var ImageID = substring(filename, startIndex+4, cropIndex);
			print(ImageID);
	
			// (optional) Combine raw channel images into image stack, and save as hyperstack (component image)
			if (channelsTOComponent == true) {
				
				// Define file paths and open each raw channel image  
				var ch00Img = compositeFolder + "/" + ch00list[img];
				var ch01Img = compositeFolder + "/" + ch01list[img];
				var ch02Img = compositeFolder + "/" + ch02list[img];
				
					open(ch00Img);
						run("Maximize");	
						var ch00 = getTitle();
					open(ch01Img);
						run("Maximize");	
						var ch01 = getTitle();
					open(ch02Img);
						run("Maximize");	
						var ch02 = getTitle();
					
				var componentImgName = ImageID + "_stack";
				run("Images to Stack", "name="+ componentImgName +" keep");
				// run("Stack to Hyperstack...", "order=xyczt(default) channels=3 slices=1 frames=1 display=Color");
				saveAs("tiff", dir + "/01_WSI/" + markerPanel + "_overlay" + "/" + componentImgName + ".tif");

					// merge channels with defined colour LUT via z projection 
					run("Merge Channels...", "c2=["+ch01+"] c3=["+ch00+"] c7=["+ch02+"] create keep");
					rename("composite_YGB");
					saveAs("tiff", dir + "/01_WSI/" + markerPanel + "_overlay" + "/" + ImageID + "_ch127.tif");
					run("Merge Channels...", "c1=["+ch02+"] c2=["+ch01+"] c3=["+ch00+"] create keep");
					rename("composite_RGB");
					saveAs("tiff", dir + "/01_WSI/" + markerPanel + "_overlay" + "/" + ImageID + "_ch123.tif");	
			}
		
		// open the LAS X exported ch00+ch02 overlay composite image (the imagelist[img] in loop) 
		var imagePath = compositeFolder + "/" + filename;
		open(imagePath); 	
		run("Maximize");	
		var composite_overlay = getTitle();
		
		// loop through stackList to search for and open the component .tif with raw histogram of each channel (exported directly from .xlef)   
		var matchName = ImageID + "-component_ORG.tif";
		    for (stack=0; stack<stackList.length; stack++) {
		    	if (matches(stackList[stack], matchName)) {
				    var componentPath = componentFolder + "/" + stackList[stack];
					open(componentPath); 
					run("Maximize");	
				    var component_stack = getTitle();
				    var nChannels = nSlices;
				    break;
		    	}
		    }
	    
	    print("Found " + component_stack + " matched with " + composite_overlay);

	
			// re-calibration based on the target image resolution (PhenoImager-acquired)
			run("Set Scale...", "distance=0 known=0 unit=pixel global");
			// run("Set Scale...", "distance=366.5 known=100 unit=um global");
			
			// get pixel dimension (w x h) of the WSI opened in viewer 
			selectImage(composite_overlay);
			var wsi_Width_px = getWidth;
			var wsi_Height_px = getHeight;
			print("Dimension of large image = " + wsi_Width_px + "px * " + wsi_Height_px + "px");
			
			// get pixel dimension (w x h) of the reference crop opened in viewer 
			selectImage(refcrop_image);
			var refcrop_Width_px = getWidth;
			var refcrop_Height_px = getHeight;
			print("Dimension of reference crop = " + refcrop_Width_px + "px * " + refcrop_Height_px + "px");
			
			// convert both dimensions to estimate in µm using their respective resolutions
			refcrop_Width_um = refcrop_Width_px/refRes;
			refcrop_Height_um = refcrop_Height_px/refRes;
			print("reference crop's image size = " + refcrop_Width_um + "um * " + refcrop_Height_um + "um");
			wsi_Width_um = wsi_Width_px/wsiRes;
			wsi_Height_um = wsi_Height_px/wsiRes;
			print("large ROI within WSI's image size = " + wsi_Width_um + "um * " + wsi_Height_um + "um");
			
			// spacing between the rectangles
			var spacing = 0; 
			var gridArea_px = refcrop_Width_px * refcrop_Height_px;
			var gridArea_um = refcrop_Width_um * refcrop_Height_um;
			print("Area of each target stamp (" + refcrop_Width_um + "px * " + refcrop_Height_um + "px) = " + gridArea_um + "µm^2 ("+ gridArea_px + "px^2)");
	
/**********************************************************************************************/
	// Use the reference image pixel dimensions to estimate how many grid rectangles (crops) can fit in by width and height of the larger ROI
		
		selectImage(composite_overlay);
		
		// maximum number of grids that will fit along the height & width of WSI by pixels  
	    var numBoxX = floor(wsi_Width_px/refcrop_Width_px); // number of grids that will fit along the width
	    var numBoxY = floor(wsi_Height_px/refcrop_Height_px); // number of grids that will fit along the height
	    
	    // remainder distance left after the maximum no. of grids fit in 
	    var remainX = (wsi_Width_px - (numBoxX*refcrop_Width_px))/2; 
	    var remainY = (wsi_Height_px - (numBoxY*refcrop_Height_px))/2;
	    
    	var numGrids = numBoxX * numBoxY;
		print("estimated to crop " + numGrids + " stamps from within this image");

		var gridIndex = 1;
		
		// draws rectangles in a grid, centred on the X and Y axes and adds to ROI manager
	    for (i=0; i<numBoxY; i++) { 
	    	for (j=0; j<numBoxX; j++) {
	    		
	    		var width = refcrop_Width_px;
	    		var height = refcrop_Height_px;
	    		
	    		// define dimensions of rectangle (x,y,w,h)
		    		if (cropXYstart == "topleft"){
		    			// start cropping from the left top corner of the image (0,0)
		    			makeRectangle((0 + (j*width)), (0 +(i*height)), width, height); 
		    		}
		    		if (cropXYstart == "remainXY"){
			    		// start cropping from (remainX,remainY) instead of the left top corner 
		    			makeRectangle((remainX + (j*width)), (remainY+(i*height)), width, height); 
		    		}
	    		roiManager("add");
	    		roiManager("Show All with labels");
				roiManager("Select", roiManager("count")-1);
				// rename each patch with double padded counter
				var roiName = "patch_" + IJ.pad(gridIndex, 2);
				roiManager("Rename", roiName);
				roiManager("Set Color", "red");
				roiManager("Set Line Width", 15);
				// (optional) export all patches regardless of Abeta+ segmentation as filter 
				if (abetaPatchOnly == false && exportPatches == true) {
				    run("Duplicate...", "title=patch"); 
				    rename(roiName);
				    saveAs("tiff", dir + "/02_patches/" + markerPanel + "/" + ImageID + "_" + roiName + ".tif");
				    close();
				}
				gridIndex++;
	    	}
	    }
	    
	    // get the total no. of patches stamped within WSI crop image
		var patchN = roiManager("count")-1;

    	// (optional) save all patch ROIs 
	 	// roiManager("Save", dir + "/04_ROIsets/" + "patches_" + ImageID + ".zip"); 
	 	
	 	// export overlay log image of all stamps with labels within composite_overlay
    	roiManager("Show All with labels");
    	run("Labels...", "color=white font=20 show bold");
    	run("Flatten");
    	wait(500);
    	saveAs("tiff", dir + "/00_stampMap/" + ImageID + "_40X_StampMap.tif");
    	close();
    	// print("saved a stamp map for all of the rectangular ROI grids.");

/**********************************************************************************************/
	
	// if user want to export plaques+ patches only, segment plaques now.
	if (abetaPatchOnly == true){
	
		// open single channel image with suffix _ch02.tif
		var abetaImagePath = compositeFolder + "/" + ch00list[img];
		open(abetaImagePath); 
		run("Maximize");	
		var AbetaCh = getTitle();
		
		// (optional) check imageID of Abeta channel if it matches with the overlay composite and component 
			var AbetaChEndIndex = indexOf(AbetaCh, "_ch");
			var AbetaChStartIndex = indexOf(AbetaCh, "40X_");
			var AbetaCh_imageID = substring(AbetaCh, AbetaChStartIndex+4, AbetaChEndIndex);
			print(AbetaCh_imageID);
		if (AbetaCh_imageID != ImageID) {print("wrong match!"); break;}

		// segment abeta plaques by thresholding
		selectImage(AbetaCh);
		run("8-bit");
		run("Threshold...");
		setThreshold(50, 255);
		run("Convert to Mask");
		run("Create Selection");
		roiManager("Add");
		var allabetaROI = roiManager("count")-1;
		roiManager("Select", allabetaROI);
		roiManager("Rename", "all plaques");
		roiManager("Set Color", "magenta");

		// loop through all patches to extract the overlapping area of Abeta from within each patch
		var abetaPatches = newArray(0);
		var abetaPatchIndexArray = newArray(0);
		var skippedPatches = newArray(0);
		
			for (p=0; p<patchN; p++) { 
				
				roiManager("Select", p);
				patchName = Roi.getName(); 
				roiManager("Select", newArray(p, allabetaROI));
				roiManager("AND");
					type=selectionType();
	            	if (type!=-1) {		// selectiontype() returns -1 if there is no overlapped selection (i.e. not colocalised)
	            	
						roiManager("Add");
						abetaPatchIndex = roiManager("count")-1;
						roiManager("Select",abetaPatchIndex);
						roiManager("Measure");
						abetaArea = getResult("Area", 0);
						if (abetaArea >abetaAreaThreshold) {
							roiManager("Rename", patchName + "_abeta+");
							abetaPatches = Array.concat(abetaPatches,patchName);
							abetaPatchIndexArray = Array.concat(abetaPatchIndexArray,abetaPatchIndex);
							
							if (saveComposite == true){
								// save patch as composite.tif image (yAbeta-DAPI RGB)
								selectImage(composite_overlay);
								roiManager("Show None");
								roiManager("Select", p);
							    run("Duplicate...", "title=" + patchName + "-composite"); 
							    saveAs("tiff", dir + "/02_patches/" + markerPanel + "_composite_40X" + "/" + ImageID + "_" + patchName + ".tif");
							    close();
							}
							
							if (saveComponent == true) {
								// save patch as component.tif image (multichannel hyperstack for QuPath analyses)
								selectImage(component_stack);
								roiManager("Select", p);
							    run("Duplicate...", "duplicate"); 
							    rename(patchName + "-component");
							    selectImage(patchName + "-component");
							    run("Stack to Hyperstack...", "order=xyczt(default) channels="+ nChannels +" slices=1 frames=1 display=Color");
							    saveAs("Tiff", dir + "/02_patches/" + markerPanel + "_component_40X" + "/" + ImageID + "_" + patchName + ".tif");
							    close();
							}
							
						} else {
							roiManager("Rename", patchName + "_abeta-");
							skippedPatches = Array.concat(skippedPatches,patchName);
						}
	            	} else {
	            		// print("skipped: no overlapped abeta pixels within " + patchName);
	            		skippedPatches = Array.concat(skippedPatches,patchName);
	            		continue;
	            	}
				if(isOpen("Results")) {close("Results");}
			}
			
		print("Among " + patchN + " patches in " + ImageID + ", " + abetaPatches.length + " patches have more than " + abetaAreaThreshold + " pixels of abeta signals, hence " + skippedPatches.length + " were not exported.");
		roiManager("Select", abetaPatchIndexArray);
		// save all ROIs as a zipped ROI set for backup, in case drawn ROIs have to be reused for other analysis
		roiManager("Save", dir + "/04_ROIsets/" + "Abeta+Patches_40X_" + ImageID + ".zip"); 

	}

/**********************************************************************************************/
	// User-defined counts of grid boxes to randomly select from within all grids  
	// not neccessarily as many boxes in the grid as specified due to the constraint of # squares not fitting the width/height
	
	if (randomSelectPatches == true) {
	    var boxID = newArray(grids); 
	    var abetaPatchN = abetaPatches.length;

		// generate random number to select a patch among all, makes sure the patch has not already been chosen
	    for (s=0; s<grids; s++) { 
	    	randNum = round(random*abetaPatchN);
	    		for (r=0; r<boxID.length; r++) {
	    			if (randNum == boxID[r]) {
	    				randNum = round(random*abetaPatchN);
	    				r=0;
	    			}
	    		}
		    boxID[s] = randNum; 
		    
		    selectWindow(composite_overlay); 
		    roiManager("select", boxID[s]);
		    patchName = Roi.getName();
		    // duplicates the random rectangle in the grid box
		    run("Duplicate...", "title=selected");
		    //waitForUser("check if plaques are present");
		    rename(ImageID + "-selected_" + IJ.pad(s+1, 2) + "_" + patchName);
		    filename = getTitle();
		    saveAs("tiff", dir + "/03_random_selections/" + markerPanel + "/" + filename + ".tif");
		    close();
	    }
	    print("random sampling DONE");
	}

	
///////////////////////////////////////////////////////////////////////////////////////
		//waitForUser("Proceed?", "Finished random patch sampling for " + ImageID);
	    roiManager("reset");
	    run("Close All");
	}	
//////////////////////////////////////////////////////////////////////////
	waitForUser("Finished all images and check summary measures", "End?");
  		
	selectWindow("Log");
		timeNow = call("java.time.Instant.now");
		timeNowString = substring(timeNow,0,16);
		timeNowString = replace(timeNowString,"T", "_");
		timeNowString = replace(timeNowString,":", "");
	saveAs("Text", dir + "/Log_" + timeNowString + "_" + markerPanel + "_stampPatches.txt");
	run("Close");
	run("Close All");
	roiManager("reset");

}