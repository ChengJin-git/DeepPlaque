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

// rename the classes of all plaques into a class of 'abeta' for the subsequent NND analysis
// Run the script 07_MeasurementExporter(abeta) before running this script to obatin the class of each DL-predicted plaque

         def boundary = getAnnotationObjects().findAll {it.getLevel() == 2}
                boundary.each { it ->
                    it.setPathClass(getPathClass("abeta"))
                    it.setLocked(true)
                }  
                resolveHierarchy()
               fireHierarchyUpdate()