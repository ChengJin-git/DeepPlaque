// Import QuPath's dependencies of relevance
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.scripting.QP
import qupath.lib.gui.commands.Commands
import qupath.lib.roi.ROIs
import qupath.lib.roi.RoiTools
import qupath.lib.objects.PathObjects
import qupath.lib.objects.classes.PathClassTools
import qupath.lib.regions.ImagePlane
import qupath.imagej.gui.ImportRoisCommand
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport
import qupath.lib.objects.PathObjects
import qupath.lib.objects.PathObjectTools
import qupath.lib.roi.PolygonROI
import qupath.lib.images.servers.PixelCalibration


    // Prep: ensure image type is immunofluorescence 
    setImageType('FLUORESCENCE');
        
    // Clear all original objects in hierarchy, to avoid duplicates. 
        if (getAnnotationObjects().size()>0) {
        clearAnnotations();
        clearDetections();
        clearAllObjects();
        }
           
        // create FullimageROI as the top of the object hierarchy, as prerequisite for cell detection and further analysis 
            createFullImageAnnotation(true)
            resolveHierarchy()
                def fullROI = getAnnotationObjects().findAll {it.getLevel() == 1}
                fullROI.each { it ->
                    it.setName("FullimageROI") 
                    it.setPathClass(getPathClass("border"))
                    it.setLocked(true)
                } 
