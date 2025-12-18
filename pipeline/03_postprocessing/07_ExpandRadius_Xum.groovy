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

// Define the 3 classes of plaque
        def annoClass1 = "diffuse"
        def annoClass2 = "fibrillar"
        def annoClass3 = "cored"

// Expand radius from plaque perimeter by X um        
     
selectObjectsByClassification(annoClass1);
runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": 75.0,  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": true}');
  

selectObjectsByClassification(annoClass2);
runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": 75.0,  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": true}');

               
                selectObjectsByClassification(annoClass3);
runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": 75.0,  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": true}');

// Resolve Hierarchy so the expanded perimeter annotation is at level 2, then rename the class as 'Outer_X'
// * must run the expansion radius from highest to lowest and then escalate the hierarchy level accordingly

        resolveHierarchy()
               fireHierarchyUpdate()
               
         def boundary = getAnnotationObjects().findAll {it.getLevel() == 2}
                boundary.each { it ->
                    it.setPathClass(getPathClass("Outer_75"))
                    it.setLocked(true)
                } 
                
                resolveHierarchy()
               fireHierarchyUpdate()