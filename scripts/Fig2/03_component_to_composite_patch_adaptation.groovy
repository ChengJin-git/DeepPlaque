/* reset histogram of channel intensities for visual inspection based on true signals */

import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
import qupath.lib.gui.commands.Commands
import qupath.lib.images.servers.ImageServerMetadata
import qupath.lib.display.ImageDisplay
import qupath.lib.gui.images.servers.RenderedImageServer
import qupath.lib.gui.viewer.OverlayOptions
import qupath.lib.gui.viewer.overlays.HierarchyOverlay

///////////////////////////////////////////////////////////////////////////////////////////////////

   // Prep: ensure image type is multi-channel IF
   setImageType('FLUORESCENCE');
   
   // configure the data source of qpproj file to save
    def qupath = getQuPath()
    def project = qupath.getProject()
    def imageData = getCurrentImageData()
    def entry = getProjectEntry()
   
   // Define current image viewer --> create full image annotation for image export 
    def viewer = getCurrentViewer()
    def display = viewer.getImageDisplay()
    
    def channels = display.availableChannels()  
    println "This image has ${channels.size()} channels "
    
    def channelNames = getCurrentServer().getMetadata().getChannels().collect { c -> c.name }
    println channelNames 

        // loop through channels to reset display range for each channel
        for (int i = 0; i < channels.size(); i++) {
            
            display.selectedChannels.setAll(channels[i])
            
            // Get the max display value and reset it to max allowed display value of such channel
            // Set the maximum permissible range for the image display, opt out the default auto-set saturation on display range
            if (channels[i].getMaxDisplay() != channels[i].getMaxAllowed()) {
                String chName = channels[i]
                double autoMax = channels[i].getMaxDisplay()
                double min = channels[i].getMinAllowed()
                double max = channels[i].getMaxAllowed()
                setChannelDisplayRange(chName, min, max)
                println "reset ${chName} from ${autoMax} to ${max}" 
            }
        }
        
       // extract ID and imageID from the filename         
        def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
        def idIndex = name.indexOf("_")
        def ID = name.substring(0,idIndex)
        def cropIndex = name.indexOf("_patch")
        def cropID = name.substring(0,cropIndex)
        def patchNum = name.substring(cropIndex+7, name.length())
        def cropIDpatch = cropID + "-" + patchNum
        println cropID
        println patchNum
        
        entry.putMetadataValue("ID-sample", cropID)
        entry.putMetadataValue("cropID-patchN", cropIDpatch)

/******* User inputs****************************************************************/        

    // By default, DAPI channel is in blue colour 
    def DAPIch = channels[2]
        DAPIch.setLUTColor(0, 0, 255)
        
    // Define Abeta channel by index in channel list
    def AbetaChannelIndex = channels[0]
    // define the channel(s) to view/hide in viewer display e.g. DAPI = channel 1 (C1) has index 0 
    def AbetaChannelIndexShow1 = AbetaChannelIndex
    def AbetaChannelIndexShow2 = channels[0,2]
    
    // Define output folder paths 
    boolean exportOriginalPxl = true
    boolean exportBBOX = false
    boolean exportAbetaRaw = false 

    def outputParentFolder = "/path/to/your/output/folder"  // TODO: Set your output folder path
    def outputDir = buildFilePath(outputParentFolder)
    
    // ---------------------------------------------------------------------------------
    // COLOR CONFIGURATION FOR COMPOSITE IMAGE EXPORT
    // ---------------------------------------------------------------------------------
    // The color pair below is set to "yellow-blue" for demonstration purposes.
    // You can customize these colors based on your imaging setup and preference.
    //
    // Available color options for Abeta channel:
    //   "white", "red", "green", "yellow", "magenta", "cyan"
    //
    // Corresponding color pair names (Abeta-DAPI):
    //   "grayscale_white-blue", "red-blue", "green-blue", "yellow-blue", "magenta-blue", "cyan-blue"
    //
    // To export multiple color variants at once, uncomment and modify the lists below:
    // def AbetaColourList = ["white", "red", "green", "yellow", "magenta", "cyan"]
    // def colourPairList = ["grayscale_white-blue", "red-blue", "green-blue", "yellow-blue", "magenta-blue", "cyan-blue"]
    // ---------------------------------------------------------------------------------
    def AbetaColourList = ["yellow"]
    def colourPairList = ["yellow-blue"]
    
        // User-defined view of overlaying elements onto image to be exported
        options = getCurrentViewer().getOverlayOptions()
        options.setOpacity(1.0)
        options.setFillDetections(false)
        options.setShowNames(false)
        options.setShowAnnotations(false)
        options.setShowDetections(false)
    
/*********************************************************************************/        

    // if user needs to export Abeta with different colours in original pixel resolution
    if (exportOriginalPxl==true) {
        
        def colourIndex = 0

        // loop through colour list to change colour accordingly 
        for (colour in AbetaColourList) {
            
            println "change Abeta channel to {$colour} as colour #{$colourIndex}"
            
                // set the pseudocolour for specific channel 
                if (colour == "white") {AbetaChannelIndex.setLUTColor(255, 255, 255)}
                if (colour == "red") {AbetaChannelIndex.setLUTColor(255, 0, 0)}
                if (colour == "green") {AbetaChannelIndex.setLUTColor(0, 255, 0)}
                if (colour == "yellow") {AbetaChannelIndex.setLUTColor(255, 255, 0)}
                if (colour == "magenta") {AbetaChannelIndex.setLUTColor(255, 0, 255)}
                if (colour == "cyan") {AbetaChannelIndex.setLUTColor(0, 255, 255)}
            
            // display DAPI + Abeta channel
            display.selectedChannels.setAll(AbetaChannelIndexShow2)
            viewer.repaintEntireImage()
            
                // no downsampling with full resolution
                double downsample = 1.0  
                // Create a rendered server that includes a hierarchy overlay using the current display settings
                def server = new RenderedImageServer.Builder(imageData)
                    .display(viewer.getImageDisplay())
                    .downsamples(downsample)
                    .layers(new HierarchyOverlay(viewer.getImageRegionStore(), viewer.getOverlayOptions(), imageData))
                    .build()

            // subfolder name mapping 
            def subfolderName = colourPairList[colourIndex]

            // Write the 2-channels image with original pixel resolution and size 
            def OrginialImgPath = buildFilePath(outputDir, subfolderName, cropIDpatch + "_composite_image.tif")
            println OrginialImgPath
            writeImage(server, OrginialImgPath)
            
            colourIndex++
        }
    }
        
    if (exportAbetaRaw = true) {
        
        // reset Abeta channel in grayscale
        AbetaChannelIndex.setLUTColor(255, 255, 255)
        display.selectedChannels.setAll(AbetaChannelIndexShow1)
        viewer.repaintEntireImage()
        
            // no downsampling with full resolution
            double downsample = 1.0  
            // Create a rendered server that includes a hierarchy overlay using the current display settings
            def server = new RenderedImageServer.Builder(imageData)
                .display(viewer.getImageDisplay())
                .downsamples(downsample)
                .layers(new HierarchyOverlay(viewer.getImageRegionStore(), viewer.getOverlayOptions(), imageData))
                .build()
                    
        // updates the overlay using the current display settings
//        def displayLatest = viewer.getImageDisplay()
//        server.display(displayLatest)
        
        def rawAbetaImgPath = buildFilePath(outputDir, "whiteAbeta", cropIDpatch + "_composite_image.tif")
        writeImage(server, rawAbetaImgPath)
    }

    // reset Abeta channel in grayscale for easier visual inspection
    AbetaChannelIndex.setLUTColor(255, 255, 255)
    entry.saveImageData(imageData)
    
    // update the temp project file
    qupath.refreshProject()
    
    // save the project.qpproj file 
    project.syncChanges()