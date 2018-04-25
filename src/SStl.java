/**
 * SStl : Clase que permite cargar geometrías con formato *.stl
 *
 * @author: Jon Martinez Garcia (www.jonmartinezgarcia.neositios.com)(samylabs)
 */
package samy.cad; //Computer Aided design

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import samy.scene.SScene;
import samy.math.SMatrix;
import samy.math.SNumeric;
import samy.objects.SBox;
import samy.objects.SInteger;
import samy.external.SJava;
import samy.objects.SStyle;
import samy.objects2D.SPoint2D;
import samy.objects3D.SFace3D;
import samy.objects3D.SFaces3D;
import samy.objects3D.SLines3D;
import samy.objects3D.SObject3D;
import samy.objects3D.SObjects3D;
import samy.objects3D.SPoint3D;
import samy.objects3D.SShape3D;
import samy.objects3D.SShapes3D;
import samy.objects3D.SVertex3D;

public class SStl {

    private SObjects3D objects3d;
    private SStyle style = new SStyle(128, 218, 128, 255);
    public SInteger progress;

    /**
     * Constructor
     */
    public SStl(String path) {
        this.loadStl(path, true, true, SObjects3D.angleLimit);
    }

    /**
     * Constructor
     */
    public SStl(String path, boolean enableVertexsNormals, boolean enableEdges, double angleLimit, SInteger progress) {
        this.progress = progress;
        this.loadStl(path, enableVertexsNormals, enableEdges, angleLimit);
    }

    /**
     * Constructor
     */
    public SStl(String[] lines, boolean enableVertexsNormals, boolean enableEdges, double angleLimit, SInteger progress) {
        this.progress = progress;
        this.processStlFile(lines, enableVertexsNormals, enableEdges, angleLimit);
    }

    /**
     * Constructor de copia
     */
    public SStl(SStl stl) {
        this.objects3d = stl.objects3d.getCopy();
    }

    /**
     * Estabelcer el estilo
     */
    public void setStyle(SStyle style) {
        this.style = style;
    }

    /**
     * Obtener una copia del objeto
     */
    public SStl getCopy() {
        return new SStl(this);
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
     * Función de carga para geometrias de tipo *.stl
     */
    private void loadStl(String path, boolean enableVertexsNormals, boolean enableEdges, double angleLimit) {
        String pathInLowercase = path.toLowerCase();
        if (pathInLowercase.contains(".stl")) {
            loadStlFile(path, enableVertexsNormals, enableEdges, angleLimit);
        }
    }

    /**
     * Función especifica para la carga de ficheros *.stl
     */
    protected boolean loadStlFile(String path, boolean enableVertexsNormals, boolean enableEdges, double angleLimit) {
        setProgressValue(0);
        String[] lines = SJava.loadStrings(path);
        setProgressValue(10);
        if (lines != null) {
            if (isBinary(path)) {
                return processBinaryFile(path, enableVertexsNormals, enableEdges, angleLimit);
            } else {
                processStlFile(lines, enableVertexsNormals, enableEdges, angleLimit);
                return true;
            }
        } else {
            System.out.println("file" + path + "not found");
            return false;
        }
    }

    /**
     * Función especifica para procesar ficheros *.stl
     */
    private void processStlFile(String[] lines, boolean enableVertexsNormals, boolean enableEdges, double angleLimit) {
        this.objects3d = new SObjects3D();

        String line;
        int lineIndex = 0;
        int nlines = lines.length;

        //Recorremos el fichero
        while (true) {
            if (lineIndex >= lines.length) {
                break;
            }
            line = lines[lineIndex];
            lineIndex++;

            //Recorremos el solido
            if (line.contains("solid")) {
                SFaces3D faces = new SFaces3D();
                while (true) {
                    if (lineIndex >= lines.length) {
                        break;
                    }
                    line = lines[lineIndex];
                    lineIndex++;
                    setProgressValue((int) (10 + 70 * (float) ((float) lineIndex / (float) nlines)));

                    //Recorremos la cara
                    SFace3D face = new SFace3D();
                    SPoint3D normal = new SPoint3D();
                    int ifacet = line.indexOf("facet normal");
                    if (ifacet != -1) {
                        String lineFacet = line.substring(ifacet);
                        String facetFields[] = lineFacet.split(" ");
                        if (facetFields.length == 5) {
                            normal.x = SNumeric.eval(facetFields[2]);
                            normal.y = SNumeric.eval(facetFields[3]);
                            normal.z = SNumeric.eval(facetFields[4]);
                        }

                        while (true) {
                            if (lineIndex >= lines.length) {
                                break;
                            }
                            line = lines[lineIndex];
                            lineIndex++;
                            setProgressValue((int) (10 + 70 * (float) ((float) lineIndex / (float) nlines)));

                            //Recorremos los vertices de la cara
                            if (line.contains("outer loop")) {
                                while (true) {
                                    if (lineIndex >= lines.length) {
                                        break;
                                    }
                                    line = lines[lineIndex];
                                    lineIndex++;

                                    int ivertex = line.indexOf("vertex");
                                    if (ivertex != -1) {
                                        String lineVertex = line.substring(ivertex);
                                        String vertexFields[] = lineVertex.split(" ");
                                        if (vertexFields.length == 4) {
                                            SVertex3D vertex = new SVertex3D();
                                            vertex.x = SNumeric.eval(vertexFields[1]);
                                            vertex.y = SNumeric.eval(vertexFields[2]);
                                            vertex.z = SNumeric.eval(vertexFields[3]);
                                            vertex.normal = normal;
                                            face.add(vertex);
                                            face.normal = normal;
                                        }
                                    }
                                    if (line.contains("endloop")) {
                                        face.styleFill = style;
                                        faces.add(face);
                                        break;
                                    }
                                }
                            }
                            if (line.contains("endfacet")) {
                                break;
                            }
                        }
                    }
                    if (line.contains("endsolid")) {
                        break;
                    }
                }
                //5. Recorremos las lineas
                setProgressValue(80);

                //6. Si procede calculamos las normales de vertices y las aristas
                SLines3D edges = faces.computeNormalsAndEdges(enableEdges, enableVertexsNormals, angleLimit);
                edges.setStyle(new SStyle(0, 0, 0, 255, 2));
                setProgressValue(90);

                //7.Creamos el objeto3d
                SObject3D object3d = new SObject3D(faces, edges);
                objects3d.add(object3d);
                setProgressValue(100);
            }
        }
    }

    /**
     * Evalua si el fichero STL es binario.
     *
     * @returns true si y solo si se puede confirmar que el fichero es binario.
     *
     */
    boolean isBinary(String path) {
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            byte[] header = new byte[80];
            int readed = in.read(header);
            in.close();
            if (readed != 80) {
                return false;
            }
            String solid = new String(header, 0, 5);
            return !solid.equals("solid");
        } catch (IOException ex) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // nothing to do here
                }
            }
        }
    }

    /**
     * Importacion de STL binario la definicion del formato esta en :
     * https://en.wikipedia.org/wiki/STL_(file_format)
     *
     * @param file
     * @param enableVertexsNormals
     * @param enableEdges
     * @param edgesAngleLimit
     * @param progress
     * @return
     */
    protected boolean processBinaryFile(String path, boolean enableVertexsNormals, boolean enableEdges, double angleLimit) {
        double nx, ny, nz;
        double x1, y1, z1;
        double x2, y2, z2;
        double x3, y3, z3;
        byte[] attribute = new byte[2];
        int ntriangles = 0;
        int n = 0;
        this.objects3d = new SObjects3D();

        SFaces3D faces = new SFaces3D();
        InputStream is = null;
        try {
            is = new FileInputStream(path);
            Long skipped = is.skip(80L); // pasamos de la cabecera. 

            ByteBuffer in = ByteBuffer.allocate(4);
            in.order(ByteOrder.LITTLE_ENDIAN);
            int l = is.read(in.array());
            ntriangles = in.getInt();

            in = ByteBuffer.allocate(50); // 12*4+2
            in.order(ByteOrder.LITTLE_ENDIAN);
            // lee cada triangulo   

            while (is.read(in.array()) == 50) {
                nx = in.getFloat();
                ny = in.getFloat();
                nz = in.getFloat();
                x1 = in.getFloat();
                y1 = in.getFloat();
                z1 = in.getFloat();
                x2 = in.getFloat();
                y2 = in.getFloat();
                z2 = in.getFloat();
                x3 = in.getFloat();
                y3 = in.getFloat();
                z3 = in.getFloat();
                in.get(attribute);
                in.position(0);
                // si ha leido todo, lo metemos en los triangulos.  
                SFace3D face = new SFace3D();
                SPoint3D normal = new SPoint3D();
                normal.x = nx;
                normal.y = ny;
                normal.z = nz;
                SVertex3D vertex1 = new SVertex3D(x1, y1, z1, nx, ny, nz); //TODO: ¿NO sobran las normales dentro de los vertices?
                SVertex3D vertex2 = new SVertex3D(x2, y2, z2, nx, ny, nz);
                SVertex3D vertex3 = new SVertex3D(x3, y3, z3, nx, ny, nz);
                face.add(vertex1);
                face.add(vertex2);
                face.add(vertex3);
                face.normal = normal;
                face.styleFill = style;
                faces.add(face);
                n++;
                setProgressValue((int) (10 + 70 * (float) ((float) n / (float) ntriangles)));
            }
        } catch (IOException ex) {
            return false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    // nothing to do here
                }
            }
        }

        setProgressValue(80);
        if (n != ntriangles) { // el resultado tiene que ser consistente entre los triangulos que declara el fichero y los obtenidos.			
            return false;
        }

        //6. Si procede calculamos las normales de vertices y las aristas
        SLines3D edges = faces.computeNormalsAndEdges(enableEdges, enableVertexsNormals, angleLimit);
        edges.setStyle(new SStyle(0, 0, 0, 255, 2));
        setProgressValue(90);

        //7.Creamos el objeto3d
        SObject3D object3d = new SObject3D(faces, edges);
        objects3d.add(object3d);
        setProgressValue(100);
        return true;
    }

    /**
     * Trasladar
     */
    public void translate(SPoint2D p) {
        this.translate(p.x, p.y, 0);
    }

    /**
     * Trasladar
     */
    public void translate(SPoint3D p) {
        this.translate(p.x, p.y, p.z);
    }

    /**
     * Trasladar
     */
    public void translate(double x, double y) {
        this.translate(x, y, 0);
    }

    /**
     * Trasladar
     */
    public void translate(double x, double y, double z) {
        this.objects3d.translate(x, y, z);
    }

    /**
     * Rotar alrededor de X (En radianes)
     */
    public void rotateX(double rx) {
        this.objects3d.rotateX(rx);
    }

    /**
     * Rotar alrededor de Y (En radianes)
     */
    public void rotateY(double ry) {
        this.objects3d.rotateY(ry);
    }

    /**
     * Rotar alrededor de Z (En radianes)
     */
    public void rotateZ(double rz) {
        this.objects3d.rotateZ(rz);
    }

    /**
     * Escalar
     */
    public void scale(double fx, double fy, double fz) {
        this.objects3d.scale(fx, fy, fz);
    }

    /**
     * Aplicar una matriz de transformacion
     */
    public void transform(SMatrix M) {
        this.objects3d.transform(M);
    }

    /**
     * Ajustar el objeto a las dimensiones del objeto sBox
     */
    public void fit(SBox box) {
        SBox thisBox = this.getBox();
        double fx = box.getX() / thisBox.getX();
        double fy = box.getY() / thisBox.getY();
        double fz = box.getZ() / thisBox.getZ();
        double f = Math.min(fx, fy);
        f = Math.min(f, fz);
        this.scale(f, f, f);
        this.translate(thisBox.getPmin().getMult(-f).getAdd(box.getPmin()));
    }

    /**
     * Ajustar el objeto a las dimensiones del objeto sBox y al centro
     */
    public void fitCenter(SBox box) {
        SBox thisBox = this.getBox();
        double fx = box.getX() / thisBox.getX();
        double fy = box.getY() / thisBox.getY();
        double fz = box.getZ() / thisBox.getZ();
        double f = Math.min(fx, fy);
        f = Math.min(f, fz);
        this.scale(f, f, f);
        this.translate((thisBox.getCenter().getMult(-f)).getAdd(box.getCenter()));
    }

    /**
     * Establecer el estilo de las aristas
     */
    public void setStyleEdges(SStyle style) {
        objects3d.setStyleEdges(style);
    }

    /**
     * Obtener la box
     */
    public SBox getBox() {
        return objects3d.getBox();
    }

    /**
     * Obtener un objeto3D
     */
    public SFaces3D getFaces3D() {
        SFaces3D faces = new SFaces3D();
        for (int i = 0; i < objects3d.size(); i++) {
            SObject3D object3d = objects3d.get(i);
            if (object3d != null) {
                faces.add(object3d.getFaces3D());
            }
        }
        return faces;
    }

    /**
     * Obtener las aristas
     */
    public SLines3D getEdges() {
        SLines3D edges = new SLines3D();
        for (int i = 0; i < objects3d.size(); i++) {
            SObject3D object3d = objects3d.get(i);
            if (object3d != null) {
                edges.add(object3d.getEdges());
            }
        }
        return edges;
    }

    /**
     * Obtener una forma de representacion rapida
     */
    public SShape3D getShape3D() {
        return objects3d.getShape3D();
    }

    /**
     * Obtener las formas de representacion rapida
     */
    public SShapes3D getShapes3D() {
        SShapes3D shapes = new SShapes3D();
        for (int i = 0; i < objects3d.size(); i++) {
            SObject3D object3d = objects3d.get(i);
            if (object3d != null) {
                shapes.add(object3d.getShape3D());
            }
        }
        return shapes;
    }

    /**
     * Obtener los objetos 3D
     */
    public SObjects3D getObjects() {
        return this.objects3d;
    }

    /**
     * Dibuja la geometría 3d
     */
    public void draw(SScene scene) {
        objects3d.draw(scene);
    }

    /**
     * Imprimir los datos
     */
    public void print() {
        objects3d.print();
    }
}
