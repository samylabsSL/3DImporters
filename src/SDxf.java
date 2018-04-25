/**
 * SDxf : Clase que permite cargar planos de Autocad 2004 en formato *.dxf
 *
 * @author: Jon Martinez Garcia (www.jonmartinezgarcia.neositios.com)(samylabs)
 */
package samy.cad; //Computer Aided design

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import samy.objects.SObject;
import samy.scene.SScene;
import samy.graph.SGraph;
import samy.graph.SGraphNode;
import samy.math.SIntersections2D;
import samy.math.SNumeric;
import samy.objects.SBox;
import samy.objects.SInteger;
import samy.external.SJava;
import samy.objects.SStyle;
import samy.objects2D.SArc2D;
import samy.objects2D.SArcs2D;
import samy.objects2D.SCircle2D;
import samy.objects2D.SCircles2D;
import samy.objects2D.SCurve2D;
import samy.objects2D.SCurves2D;
import samy.objects2D.SEllipse2D;
import samy.objects2D.SEllipses2D;
import samy.objects2D.SLine2D;
import samy.objects2D.SLines2D;
import samy.objects2D.SPoint2D;
import samy.objects2D.SPoints2D;
import samy.objects2D.SPolygon2D;
import samy.objects2D.SPolygons2D;
import samy.objects2D.SText2D;
import samy.objects2D.STexts2D;
import samy.objects2D.STriangles2D;
import samy.objects3D.SCurve3D;
import samy.objects3D.SCurves3D;
import samy.objects3D.SFaces3D;
import samy.objects3D.SLines3D;
import samy.objects3D.SObject3D;
import samy.objects3D.SPoint3D;
import samy.objects3D.SShape3D;
import samy.objects3D.SShapes3D;

public class SDxf {

    private SGraph graph; //Grafo del dxf            
    public boolean enablePoints = true;
    public boolean enableLines = true;
    public boolean enableCircles = true;
    public boolean enableEllipses = true;
    public boolean enableArcs = true;
    public boolean enablePolylines = true;
    public boolean enableSplines = true;
    public boolean enableHatchs = true;
    public boolean enableTexts = true;
    public boolean enableBlocks = false;
    public SInteger progress;

    /**
     * Constructor
     */
    public SDxf() {
        this.graph = new SGraph();
    }

    /**
     * Constructor
     */
    public SDxf(String path) {
        this.graph = new SGraph();
        this.loadDxf(path);
    }

    /**
     * Constructor de copia
     */
    public SDxf(SDxf dxf) {
        this.graph = dxf.graph.getCopy();
    }

    /**
     * Obtener una copia del objeto
     */
    public SDxf getCopy() {
        return new SDxf(this);
    }

    /**
     * Cargador
     */
    public void load(String path) {
        this.loadDxf(path);
    }

    /**
     * Establecer la barra de progreso
     */
    public void setProgress(SInteger progress) {
        this.progress = progress;
    }

    /**
     * Obtener la barra de progreso
     */
    public SInteger getProgress() {
        return this.progress;
    }

    /**
     * Establecer el valor de la barra de progreso
     */
    public void setProgressValue(int value) {
        if (progress != null) {
            progress.value = value;
        }
    }

    /**
     * Constructor
     */
    private void loadDxf(String path) {
        setProgressValue(0);
        String pathLower = path.toLowerCase();
        if (!pathLower.contains(".dxf")) {
            System.out.println("Formato desconocido");
        } else {
            setProgressValue(10);

            //Cargamos la sección de entidades
            this.loadEntitiesSection(path, graph.getRoot());
            setProgressValue(20);

            if (enableBlocks) {
                //Cargamos la seccion de bloques
                this.loadBlocksSection(path, graph.getRoot());
            }
            setProgressValue(60);

            //Cargamos la traslación global del dxf
            this.loadGlobalTraslation(path);
            setProgressValue(100);
        }
    }

    /**
     * Cargar la seccion de entidades
     */
    private void loadEntitiesSection(String path, SGraphNode parent) {
        BufferedReader reader = SJava.createReader(new File(path));
        try {
            String str = "";

            //Buscamos el origen de la seccion de entidades
            while (!str.contains("ENTITIES")) {
                str = reader.readLine();
            }

            //Cargar todas las entidades del nodo raiz del grafo hasta que se finalice la seccion de entidades         
            loadEntities(reader, "ENDSEC", graph.getRoot());
        } catch (IOException e) {
        }
    }

    /**
     * Cargar la seccion de bloques
     */
    private void loadBlocksSection(String path, SGraphNode parent) {
        BufferedReader reader = SJava.createReader(new File(path));
        try {
            String str = "";

            //Buscamos el origen de la sección de bloques
            while (!str.contains("BLOCKS")) {
                str = reader.readLine();
            }

            //Hasta que termine la seccion de bloques
            while (!str.contains("ENDSEC")) {
                str = reader.readLine();
                if (str == null) {
                    break;
                }

                if (str.equals("BLOCK")) {
                    String nodeId = loadId(reader);
                    if (nodeId != null) {
                        SGraphNode node = new SGraphNode(nodeId, new SObject());
                        graph.addNode(parent, node);

                        //Hasta que termine el bloque de entidades
                        loadEntities(reader, "ENDBLK", node);
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    /**
     * Cargar la traslacion global del dxf
     */
    private void loadGlobalTraslation(String path) {
        BufferedReader reader = SJava.createReader(new File(path));
        try {
            String str = "";
            //Buscamos la coordenada X
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float orgX = Float.parseFloat(str);

            //Buscamos la coordenada Y
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float orgY = Float.parseFloat(str);

            //Si hay traslación trasladomos los objetos del grafo
            if (orgX != 0 || orgY != 0) {
                graph.translate(orgX, orgY, 0);
            }
        } catch (IOException e) {
        }
    }

    /**
     * Cargar el identificador
     */
    private String loadId(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos el identificador
            while (!str.contains("  5")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            String id = str;
            return id;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar el estilo
     */
    private SStyle loadStyle(String label, BufferedReader reader) {
        String str = "";
        int ncolor = 0;
        try {
            while (!str.contains(label)) {
                str = reader.readLine();
                if (str.contains(" 62")) {
                    str = reader.readLine();
                    ncolor = (int) Float.parseFloat(str);
                    break;
                }
            }
        } catch (IOException e) {
        }
        return getStyle(ncolor, 255, 1);
    }

    /**
     * Cargar las entidades de un nodo padre
     */
    private void loadEntities(BufferedReader reader, String endLabel, SGraphNode parent) {
        String str = "";

        try {
            //Hasta la etiqueta de fin
            while (!str.contains(endLabel)) {
                str = reader.readLine();
                if (str == null) {
                    break;
                }

                if (str.equals("POINT") && enablePoints) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbPoint", reader);
                    SPoint2D point = loadPoint(reader);

                    if (id != null && style != null && point != null) {
                        point.setStyle(style);
                        SGraphNode node = new SGraphNode(id, point);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("LINE") && enableLines) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbLine", reader);
                    SLine2D line = loadLine(reader);

                    if (id != null && style != null && line != null) {
                        line.setStyle(style);
                        SGraphNode node = new SGraphNode(id, line);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("CIRCLE") && enableCircles) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbCircle", reader);
                    SCircle2D circle = loadCircle(reader);

                    if (id != null && style != null && circle != null) {
                        circle.setStyleStroke(style);
                        SGraphNode node = new SGraphNode(id, circle);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("ELLIPSE") && enableEllipses) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbEllipse", reader);
                    SEllipse2D ellipse = loadEllipse(reader);

                    if (id != null && style != null && ellipse != null) {
                        ellipse.setStyleStroke(style);
                        SGraphNode node = new SGraphNode(id, ellipse);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("ARC") && enableArcs) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbCircle", reader);
                    SArc2D arc = loadArc(reader);

                    if (id != null && style != null && arc != null) {
                        arc.setStyleStroke(style);
                        SGraphNode node = new SGraphNode(id, arc);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("LWPOLYLINE") && enablePolylines) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbPolyline", reader);
                    SCurve2D polyline = loadPolyline(reader);

                    if (id != null && style != null && polyline != null) {
                        polyline.setStyle(style);
                        SGraphNode node = new SGraphNode(id, polyline);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("SPLINE") && enableSplines) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbSpline", reader);
                    SCurve2D spline = loadSpline(reader);

                    if (id != null && style != null && spline != null) {
                        spline.setStyle(style);
                        SGraphNode node = new SGraphNode(id, spline);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("HATCH") && enableHatchs) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbHatch", reader);
                    SPolygon2D hatch = loadHatch(reader);

                    if (id != null && style != null && hatch != null) {
                        hatch.setStyleFill(style);
                        SGraphNode node = new SGraphNode(id, hatch);
                        graph.addNode(parent, node);
                    }

                } else if (str.equals("TEXT") && enableTexts) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbText", reader);
                    SText2D text = loadText(reader);

                    if (id != null && style != null && text != null) {
                        text.setStyleFill(style);
                        SGraphNode node = new SGraphNode(id, text);
                        graph.addNode(parent, node);
                    }
                } else if (str.equals("MTEXT") && enableTexts) {
                    String id = loadId(reader);
                    SStyle style = loadStyle("AcDbMText", reader);
                    SText2D text = loadMText(reader);

                    if (id != null && style != null && text != null) {
                        text.setStyleFill(style);
                        SGraphNode node = new SGraphNode(id, text);
                        graph.addNode(parent, node);
                    }
                } else {
                }
            }
        } catch (IOException e) {
        }
    }

    /**
     * Cargar la entidad punto
     */
    private SPoint2D loadPoint(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float x = Float.parseFloat(str);
            //Buscamos la coordenada Y
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float y = Float.parseFloat(str);
            //Generamos el punto
            SPoint2D point = new SPoint2D(x, y);
            return point;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad linea
     */
    private SLine2D loadLine(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X del punto inicial
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float ax = Float.parseFloat(str);
            //Buscamos la coordenada Y del punto inicial
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float ay = Float.parseFloat(str);
            //Buscamos la coordenada X del punto final
            while (!str.contains(" 11")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float bx = Float.parseFloat(str);
            //Buscamos la coordenada Y del punto final
            while (!str.contains(" 21")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float by = Float.parseFloat(str);

            //Generamos los puntos inicial y final
            SPoint2D a = new SPoint2D(ax, ay);
            SPoint2D b = new SPoint2D(bx, by);
            //Generamos la linea
            SLine2D line = new SLine2D(a, b);
            return line;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad circulo
     */
    private SCircle2D loadCircle(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X del centro
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cx = Float.parseFloat(str);
            //Buscamos la coordenada Y del centro
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cy = Float.parseFloat(str);
            //Buscamos el radio del circulo
            while (!str.contains(" 40")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float r = Float.parseFloat(str);
            //Generamos el circulo
            SCircle2D circle = new SCircle2D(new SPoint2D(cx, cy), 0, r, 48);
            return circle;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad elipse
     */
    private SEllipse2D loadEllipse(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X del centro
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cx = Float.parseFloat(str);
            //Buscamos la coordenada Y del centro
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cy = Float.parseFloat(str);
            //Buscamos la coordenada X (respecto al centro) del extremo del semieje a
            while (!str.contains(" 11")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float axisX = Float.parseFloat(str);
            //Buscamos la coordenada Y (respecto al centro) del extremo del semieje a
            while (!str.contains(" 21")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float axisY = Float.parseFloat(str);
            //Buscamos el ratio entre el semieje mayor (a) y el semieje menor(b)
            while (!str.contains(" 40")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float ratio = Float.parseFloat(str);
            //Generamos la elipse
            SEllipse2D ellipse = new SEllipse2D(new SPoint2D(cx, cy), new SPoint2D(axisX, axisY), ratio, 48);
            return ellipse;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad arco
     */
    private SArc2D loadArc(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X del arco
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cx = Float.parseFloat(str);
            //Buscamos la coordenada Y del centro
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cy = Float.parseFloat(str);
            //Buscamos el radio del circulo
            while (!str.contains(" 40")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float r = Float.parseFloat(str);
            //Buscamos el angulo de inicio
            while (!str.contains(" 50")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float sa = Float.parseFloat(str);
            //Buscamos el angulo de fin
            while (!str.contains(" 51")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float ea = Float.parseFloat(str);
            //Generamos el arco
            SArc2D arc = new SArc2D(new SPoint2D(cx, cy), r, r, sa, ea, 48);
            return arc;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad polilinea
     */
    private SCurve2D loadPolyline(BufferedReader reader) {
        String str = "";
        try {
            //Obtenemos el numero de puntos de la polilinea
            while (!str.contains(" 90")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float npoints = Float.parseFloat(str);
            //Estudiamos si la polilinea es cerrada o abierta
            while (!str.contains(" 70")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float closed = Float.parseFloat(str);
            SCurve2D polyline = new SCurve2D();
            float b = 0;
            for (int i = 0; i < (int) npoints; i++) {
                //Buscamos la coordenada X del punto
                while (!str.contains(" 10")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float x = Float.parseFloat(str);

                //Buscamos la coordenada Y del punto
                while (!str.contains(" 20")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float y = Float.parseFloat(str);

                if (b != 0) {
                    SPoint2D p1 = polyline.getLast().getCopy();
                    SPoint2D p2 = new SPoint2D(x, y);
                    polyline.add(getArcFromBuldge(p1, p2, b));
                    b = 0;
                }

                //Buscamos el buldge del punto o su coordenanda X
                while (!str.contains(" 10") && !str.contains(" 42") && !str.contains(" 0")) {
                    str = reader.readLine();
                }
                if (str.contains(" 42")) {
                    str = reader.readLine();
                    b = Float.parseFloat(str);
                }
                polyline.add(new SPoint2D(x, y));
            }
            if (b != 0) {
                SPoint2D p1 = polyline.getLast().getCopy();
                SPoint2D p2 = polyline.getFirst().getCopy();
                polyline.add(getArcFromBuldge(p1, p2, b));
            }

            //Si es cerrada añadimos el primer punto
            if ((int) closed > 0) {
                polyline.add(polyline.getFirst().getCopy());
            }

            return polyline;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Obtenemos un arco a partir del buldge
     */
    public SCurve2D getArcFromBuldge(SPoint2D p1, SPoint2D p2, double b) {
        SLine2D p1p2 = new SLine2D(p1, p2);
        SPoint2D p1p2c = p1p2.getCenterPoint();
        SPoint2D p1p2vn = p1p2.getPerpendicularVector();

        double d = p1.getDistanceTo(p1p2c);
        double s = b * d;
        SPoint2D p3 = p1p2c.getAdd(p1p2vn.getMult(s));
        SPoint2D c = SIntersections2D.getCircleCenter(p1, p2, p3);
        double r = c.getDistanceTo(p1);

        SPoint2D cp1 = p1.getSub(c);
        SPoint2D cp2 = p2.getSub(c);

        double startAngle;
        double endAngle;
        if (Math.signum(s) == 1.0) {
            startAngle = SNumeric.getAngleDeg(cp1.x, cp1.y);
            endAngle = SNumeric.getAngleDeg(cp2.x, cp2.y);
        } else {
            startAngle = SNumeric.getAngleDeg(cp2.x, cp2.y);
            endAngle = SNumeric.getAngleDeg(cp1.x, cp1.y);
        }
        SArc2D arc = new SArc2D(c, r, r, startAngle, endAngle, 16);
        SCurve2D curve = arc.getContourExt();
        curve.removeFirst();
        curve.removeLast();
        if (Math.signum(s) == 1.0) {
            return curve;
        } else {
            return curve.getReverse();
        }
    }

    /**
     * Cargar el grupo de datos de la polininea En los rellenos tenemos codigos
     * diferentes
     */
    private SCurve2D loadPolylineGroupData(BufferedReader reader) {
        String str = "";
        try {
            //Obtenemos el numero de puntos de la polilinea
            while (!str.contains(" 93")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float npoints = Float.parseFloat(str);
            //Estudiamos si la polilinea es cerrada o abierta
            while (!str.contains(" 73")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float closed = Float.parseFloat(str);
            SCurve2D polyline = new SCurve2D();
            for (int i = 0; i < (int) npoints; i++) {
                //Buscamos la coordenada X del punto
                while (!str.contains(" 10")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float x = Float.parseFloat(str);
                //Buscamos la coordenada Y del punto
                while (!str.contains(" 20")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float y = Float.parseFloat(str);
                polyline.add(new SPoint2D(x, y));
            }
            //Si es cerrada añadimos el primer punto
            if ((int) closed > 0) {
                polyline.add(polyline.getFirst().getCopy());
            }
            return polyline;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad spline
     */
    private SCurve2D loadSpline(BufferedReader reader) {
        String str = "";
        try {
            //Obtenemos el grado de la curva
            while (!str.contains(" 71")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int curveDegree = (int) Float.parseFloat(str);
            //Obtenemos el numero de knots
            while (!str.contains(" 72")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int nknots = (int) Float.parseFloat(str);
            //Obtenemos el numero de puntos de control
            while (!str.contains(" 73")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int nControlPoints = (int) Float.parseFloat(str);
            //Salvamos los knots
            ArrayList<Double> knots = new ArrayList();
            for (int i = 0; i < nknots; i++) {
                while (!str.contains(" 40")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                double knot = (double) Float.parseFloat(str);
                knots.add(knot);
            }
            //Guardamos los puntos de control
            SCurve2D controlPoints = new SCurve2D();
            for (int i = 0; i < nControlPoints; i++) {
                //Buscamos la coordenada X del punto
                while (!str.contains(" 10")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float x = Float.parseFloat(str);
                //Buscamos la coordenada Y del punto
                while (!str.contains(" 20")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float y = Float.parseFloat(str);
                controlPoints.add(new SPoint2D(x, y));
            }
            //Generamos la spline
            SCurve2D spline = controlPoints.getSpline(knots, curveDegree, 24);
            return spline;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar el grupo de datos de la spline En los rellenos tenemos codigos
     * diferentes
     */
    private SCurve2D loadSplineGroupData(BufferedReader reader) {
        String str = "";
        try {
            //Obtenemos el grado de la curva
            while (!str.contains(" 94")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int curveDegree = (int) Float.parseFloat(str);
            //Obtenemos el numero de knots
            while (!str.contains(" 95")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int nknots = (int) Float.parseFloat(str);
            //Obtenemos el numero de puntos de control
            while (!str.contains(" 96")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int nControlPoints = (int) Float.parseFloat(str);
            //Salvamos los knots
            ArrayList<Double> knots = new ArrayList();
            for (int i = 0; i < nknots; i++) {
                while (!str.contains(" 40")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                double knot = (double) Float.parseFloat(str);
                knots.add(knot);
            }
            //Guardamos los puntos de control
            SCurve3D controlPoints = new SCurve3D();
            for (int i = 0; i < nControlPoints; i++) {
                //Buscamos la coordenada X del punto
                while (!str.contains(" 10")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float x = Float.parseFloat(str);
                //Buscamos la coordenada Y del punto
                while (!str.contains(" 20")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                float y = Float.parseFloat(str);
                controlPoints.add(new SPoint3D(x, y, 0));
            }
            //Generamos la spline
            SCurve2D spline = controlPoints.getSpline(knots, curveDegree, 24).getCurve2D();
            return spline;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad hatch (Relleno)
     */
    private SPolygon2D loadHatch(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X del centro
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cx = Float.parseFloat(str);
            //Buscamos la coordenada Y del centro
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float cy = Float.parseFloat(str);
            //Obtenemos el tipo de relleno (1.solid fill 0.pattern fill)
            while (!str.contains(" 70")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int fillType = (int) Float.parseFloat(str);
            //Obtenemos el numero de loops
            while (!str.contains(" 91")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int numberLoops = (int) Float.parseFloat(str);

            SCurves2D loops = new SCurves2D();
            for (int i = 0; i < numberLoops; i++) {
                SCurve2D loop = new SCurve2D();

                //Obtenemos el tipo de loop
                while (!str.contains(" 92")) {
                    str = reader.readLine();
                }
                str = reader.readLine();
                int loopType = (int) Float.parseFloat(str);

                //Si el loop es una polilinea
                if (loopType == 2) {
                    SCurve2D polyline = loadPolylineGroupData(reader);
                    loops.add(polyline);
                } else {
                    //Obtenemos el numero de edges de un loop
                    while (!str.contains(" 93")) {
                        str = reader.readLine();
                    }
                    str = reader.readLine();
                    int numberEdges = (int) Float.parseFloat(str);

                    for (int j = 0; j < numberEdges; j++) {
                        //Obtenemos el tipo de edge
                        while (!str.contains(" 72")) {
                            str = reader.readLine();
                        }
                        str = reader.readLine();
                        int edgeType = (int) Float.parseFloat(str);

                        switch (edgeType) {
                            //Linea
                            case 1: {
                                SLine2D line = loadLine(reader);
                                loop.add(line.getCurve2D());
                            }
                            break;

                            //Arco circular
                            case 2: {
                                SArc2D arc = loadArc(reader);
                                loop.add(arc.getContourExt());
                            }
                            break;

                            //Arco eliptico
                            case 3: {
                                SEllipse2D ellipse = loadEllipse(reader);
                                loop.add(ellipse.getContour());
                            }
                            break;

                            //Spline
                            case 4: {
                                SCurve2D spline = loadSplineGroupData(reader);
                                loop.add(spline);
                            }
                            break;

                            //Otros casos
                            default: {
                            }
                            break;
                        }
                    }
                    loops.add(loop);
                }

            }
            SPolygon2D polygon = new SPolygon2D(loops);
            return polygon;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad MText
     */
    private SText2D loadMText(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X del texto
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float ox = Float.parseFloat(str);
            //Buscamos la coordenada Y del texto
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float oy = Float.parseFloat(str);
            //Buscamos la altura del texto
            while (!str.contains(" 40")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float hText = Float.parseFloat(str);
            //Buscamos el ancho del texto
            while (!str.contains(" 41")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float wText = Float.parseFloat(str);
            //Buscamos la alineación del texto
            while (!str.contains(" 71")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int textAlign = (int) Float.parseFloat(str);
            //Buscamos la direccion del texto
            while (!str.contains(" 72")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            int textDirection = (int) Float.parseFloat(str);

            //Buscamos el texto y el estilo
            str = reader.readLine();
            while (!str.contains("  1")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            String text = str;

            //Buscamos la coordenada X del vector director
            while (!str.contains(" 11")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            double vx = Float.parseFloat(str);
            //Buscamos la coordenada Y del vector director
            while (!str.contains(" 21")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            double vy = Float.parseFloat(str);
            //Obtenemos la rotacion
            double textRotation = SNumeric.getAngleDeg(vx, vy);
            //Montamos el texto
            SText2D text2D = new SText2D(text, "Courier New", 10, Font.PLAIN, 1.5, 0.03);
            //Lo ajustamos a la altura
            text2D.adjustHeight(hText);
            //Lo rotamos si es necesario
            if (textRotation != 0) {
                text2D.rotateZ(textRotation * Math.PI / 180);
            }
            //Lo trasladamos si es necesario
            if (ox != 0 || oy != 0) {
                text2D.translate(ox, oy, 0);
            }
            return text2D;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cargar la entidad Text
     */
    private SText2D loadText(BufferedReader reader) {
        String str = "";
        try {
            //Buscamos la coordenada X del texto
            while (!str.contains(" 10")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float ox = Float.parseFloat(str);
            //Buscamos la coordenada Y del texto
            while (!str.contains(" 20")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float oy = Float.parseFloat(str);
            //Buscamos la altura del texto
            while (!str.contains(" 40")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            float hText = Float.parseFloat(str);

            //Buscamos el texto y el estilo
            str = reader.readLine();
            while (!str.contains("  1")) {
                str = reader.readLine();
            }
            str = reader.readLine();
            String text = str;

            //Buscamos la rotacion del texto
            float textRotation = 0;
            str = reader.readLine();
            if (str.contains(" 50")) {
                str = reader.readLine();
                textRotation = Float.parseFloat(str);
            }
            //Montamos el texto
            SText2D text2D = new SText2D(text, "Courier New", 10, Font.PLAIN, 1.5, 0.03);
            //Ajustamos el texto en altura
            text2D.adjustHeight(hText);
            //Rotamos el texto si es necesario
            if (textRotation != 0) {
                text2D.rotateZ(textRotation * Math.PI / 180);
            }
            //Lo trasladamos si es necesario
            if (ox != 0 || oy != 0) {
                text2D.translate(ox, oy, 0);
            }
            return text2D;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Obtener el estilo del objeto. Le pasamos el numero de color, la
     * trasparencia, el espesor y el smooth
     */
    public SStyle getStyle(int ncolor, int a, int w) {
        SStyle style;
        switch (ncolor) {
            case 0: {
                style = new SStyle(255, 255, 255, a, w);
            }
            break;
            case 1: {
                style = new SStyle(255, 0, 0, a, w);
            }
            break;
            case 2: {
                style = new SStyle(255, 255, 0, a, w);
            }
            break;
            case 3: {
                style = new SStyle(0, 255, 0, a, w);
            }
            break;
            case 4: {
                style = new SStyle(0, 255, 255, a, w);
            }
            break;
            case 5: {
                style = new SStyle(0, 0, 255, a, w);
            }
            break;
            case 6: {
                style = new SStyle(255, 0, 255, a, w);
            }
            break;
            case 7: {
                style = new SStyle(255, 255, 255, a, w);
            }
            break;
            case 8: {
                style = new SStyle(128, 128, 128, a, w);
            }
            break;
            case 9: {
                style = new SStyle(190, 190, 190, a, w);
            }
            break;
            case 11: {
                style = new SStyle(255, 123, 123, a, w);
            }
            break;
            case 21: {
                style = new SStyle(255, 156, 123, a, w);
            }
            break;
            case 31: {
                style = new SStyle(255, 189, 123, a, w);
            }
            break;
            case 41: {
                style = new SStyle(255, 222, 183, a, w);
            }
            break;
            case 51: {
                style = new SStyle(255, 255, 123, a, w);
            }
            break;
            case 61: {
                style = new SStyle(222, 255, 123, a, w);
            }
            break;
            case 71: {
                style = new SStyle(189, 255, 123, a, w);
            }
            break;
            case 81: {
                style = new SStyle(156, 255, 123, a, w);
            }
            break;
            case 91: {
                style = new SStyle(123, 255, 123, a, w);
            }
            break;
            case 101: {
                style = new SStyle(122, 255, 156, a, w);
            }
            break;
            case 111: {
                style = new SStyle(123, 255, 189, a, w);
            }
            break;
            case 121: {
                style = new SStyle(123, 255, 222, a, w);
            }
            break;
            case 131: {
                style = new SStyle(123, 255, 255, a, w);
            }
            break;
            case 141: {
                style = new SStyle(123, 222, 255, a, w);
            }
            break;
            case 151: {
                style = new SStyle(123, 189, 255, a, w);
            }
            break;
            case 161: {
                style = new SStyle(123, 156, 255, a, w);
            }
            break;
            case 171: {
                style = new SStyle(123, 123, 255, a, w);
            }
            break;
            case 181: {
                style = new SStyle(156, 123, 255, a, w);
            }
            break;
            case 191: {
                style = new SStyle(189, 123, 255, a, w);
            }
            break;
            case 201: {
                style = new SStyle(222, 123, 255, a, w);
            }
            break;
            case 211: {
                style = new SStyle(255, 123, 255, a, w);
            }
            break;
            case 221: {
                style = new SStyle(255, 123, 222, a, w);
            }
            break;
            case 231: {
                style = new SStyle(255, 123, 189, a, w);
            }
            break;
            case 241: {
                style = new SStyle(255, 123, 156, a, w);
            }
            break;
            case 251: {
                style = new SStyle(41, 41, 41, a, w);
            }
            break;
            case 252: {
                style = new SStyle(90, 90, 90, a, w);
            }
            break;
            case 253: {
                style = new SStyle(136, 136, 136, a, w);
            }
            break;
            case 254: {
                style = new SStyle(181, 181, 181, a, w);
            }
            break;
            default: {
                style = new SStyle(255, 255, 255, a, w);
            }
        }
        return style;
    }

    /**
     * Obtener el grafo del dibujo
     */
    public SGraph getGraph() {
        return graph;
    }

    /**
     * Obtener todos los puntos
     */
    public SPoints2D getPoints2D() {
        SPoints2D points = new SPoints2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Point2D
                if (node.getObject() instanceof SPoint2D) {
                    SPoint2D point = (SPoint2D) node.getObject();
                    points.add(point);
                }
            }
        }
        return points;
    }

    /**
     * Obtener todas las lineas
     */
    public SLines2D getLines2D() {
        SLines2D lines = new SLines2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Line2D
                if (node.getObject() instanceof SLine2D) {
                    SLine2D line = (SLine2D) node.getObject();
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    /**
     * Obtener todos los circulos
     */
    public SCircles2D getCircles2D() {
        SCircles2D circles = new SCircles2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Circle2D
                if (node.getObject() instanceof SCircle2D) {
                    SCircle2D circle = (SCircle2D) node.getObject();
                    circles.add(circle);
                }
            }
        }
        return circles;
    }

    /**
     * Obtener todas las elipses
     */
    public SEllipses2D getEllipses2D() {
        SEllipses2D ellipses = new SEllipses2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Ellipse2D
                if (node.getObject() instanceof SEllipse2D) {
                    SEllipse2D ellipse = (SEllipse2D) node.getObject();
                    ellipses.add(ellipse);
                }
            }
        }
        return ellipses;
    }

    /**
     * Obtener todas los arcos
     */
    public SArcs2D getArcs2D() {
        SArcs2D arcs = new SArcs2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Arc2D
                if (node.getObject() instanceof SArc2D) {
                    SArc2D arc = (SArc2D) node.getObject();
                    arcs.add(arc);
                }
            }
        }
        return arcs;
    }

    /**
     * Obtener todas las polilineas (y splines)
     */
    public SCurves2D getPolylines2D() {
        SCurves2D polylines = new SCurves2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Curve2D
                if (node.getObject() instanceof SCurve2D) {
                    SCurve2D polyline = (SCurve2D) node.getObject();
                    polylines.add(polyline);
                }
            }
        }
        return polylines;
    }

    /**
     * Obtener todos los poligonos
     */
    public SPolygons2D getPolygons2D() {
        SPolygons2D polygons = new SPolygons2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Polygon2D
                if (node.getObject() instanceof SPolygon2D) {
                    SPolygon2D polygon = (SPolygon2D) node.getObject();
                    polygons.add(polygon);
                }
            }
        }
        return polygons;
    }

    /**
     * Obtener todos los textos
     */
    public STexts2D getTexts2D() {
        STexts2D texts = new STexts2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();

            if (node.getObject() != null) {
                //Text2D
                if (node.getObject() instanceof SText2D) {
                    SText2D text = (SText2D) node.getObject();
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    /**
     * Obtener todas las curvas en 2D
     */
    public SCurves2D getCurves2D() {
        SCurves2D curves = new SCurves2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();
            if (node.getObject() != null) {
                if (node.getObject() instanceof SCurve2D) {
                    SCurve2D curve = (SCurve2D) node.getObject();
                    curves.add(curve);
                } else if (node.getObject() instanceof SLine2D) {
                    SLine2D line = (SLine2D) node.getObject();
                    SCurve2D curve = new SCurve2D();
                    curve.add(line.a);
                    curve.add(line.b);
                    curve.setStyle(line.getStyle().getCopy());
                    curves.add(curve);
                } else if (node.getObject() instanceof SCircle2D) {
                    SCircle2D circle = (SCircle2D) node.getObject();
                    SCurve2D curve = new SCurve2D(circle.getContourExt());
                    curve.setStyle(circle.getStyleStroke().getCopy());
                    curves.add(curve);
                } else if (node.getObject() instanceof SEllipse2D) {
                    SEllipse2D ellipse = (SEllipse2D) node.getObject();
                    SCurve2D curve = new SCurve2D(ellipse.getContour());
                    curve.setStyle(ellipse.getStyleStroke().getCopy());
                    curves.add(curve);
                } else if (node.getObject() instanceof SArc2D) {
                    SArc2D arc = (SArc2D) node.getObject();
                    SCurve2D curve = new SCurve2D(arc.getContourExt());
                    curve.setStyle(arc.getStyleStroke().getCopy());
                    curves.add(curve);
                }
            }
        }
        return curves;
    }

    /**
     * Obtener todos los triangulos
     */
    public STriangles2D getTriangles2D() {
        STriangles2D triangles = new STriangles2D();

        //Polygons2D
        triangles.add(getPolygons2D().getTriangles2D());

        //Text2D        
        triangles.add(getTexts2D().getTriangles2D());

        return triangles;
    }

    /**
     * Obtener todas las curvas en 3D
     */
    public SCurves3D getCurves3D() {
        return getCurves2D().getCurves3D();
    }

    /**
     * Obtener la Box
     */
    public SBox getBox() {
        return graph.getBox();
    }

    /**
     * Obtener un objeto3D
     */
    public SObject3D getObject3D() {
        SFaces3D faces = getTriangles2D().getFaces3D();
        SLines3D edges = getCurves3D().getSegments();
        return new SObject3D(faces, edges);
    }

    /**
     * Obtener una forma de representacion rapida
     */
    public SShape3D getShape3D() {
        SShape3D shape = new SShape3D();
        shape.add(getCurves3D().getShape3D());
        shape.add(getTriangles2D().getShape3D());
        return shape;
    }

    /**
     * Obtener un conjunto de formas de representacion rapida
     */
    public SShapes3D getShapes3D() {
        SCurves2D curves = new SCurves2D();
        SLines2D lines = new SLines2D();
        for (Map.Entry me : graph.getMapNodes().entrySet()) {
            SGraphNode node = (SGraphNode) me.getValue();
            if (node.getObject() != null) {
                if (node.getObject() instanceof SCurve2D) {
                    SCurve2D curve = (SCurve2D) node.getObject();
                    curves.add(curve);
                } else if (node.getObject() instanceof SLine2D) {
                    SLine2D line = (SLine2D) node.getObject();
                    lines.add(line.getCopy());
                } else if (node.getObject() instanceof SCircle2D) {
                    SCircle2D circle = (SCircle2D) node.getObject();
                    SCurve2D curve = new SCurve2D(circle.getContourExt());
                    curve.setStyle(circle.getStyleStroke().getCopy());
                    curves.add(curve);
                } else if (node.getObject() instanceof SEllipse2D) {
                    SEllipse2D ellipse = (SEllipse2D) node.getObject();
                    SCurve2D curve = new SCurve2D(ellipse.getContour());
                    curve.setStyle(ellipse.getStyleStroke().getCopy());
                    curves.add(curve);
                } else if (node.getObject() instanceof SArc2D) {
                    SArc2D arc = (SArc2D) node.getObject();
                    SCurve2D curve = new SCurve2D(arc.getContourExt());
                    curve.setStyle(arc.getStyleStroke().getCopy());
                    curves.add(curve);
                }
            }
        }

        SShapes3D shapes = new SShapes3D();
        shapes.add(lines.getLines3D().getShape3D());
        shapes.add(curves.getCurves3D().getShapes3D());
        shapes.add(getTriangles2D().getShape3D());
        return shapes;
    }

    /**
     * Dibujar
     */
    public void draw(SScene scene) {
        graph.draw(scene, graph.getRoot());
    }

    /**
     * Imprimir los nodos
     */
    public void printNodes() {
        graph.printNodes();
    }

    /**
     * Imprimir el grafo del dxf
     */
    public void print() {
        graph.print(graph.getRoot());
    }
}
