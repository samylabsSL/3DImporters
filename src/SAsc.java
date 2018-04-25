/**
 * SAsc : Clase que permite cargar una geometría con extension *.asc
 *
 * @author: Jon Martinez Garcia (www.jonmartinezgarcia.neositios.com)(samylabs)
 */
package samy.cad; //Computer Aided design

import samy.scene.SScene;
import samy.math.SMatrix;
import samy.math.SNumeric;
import samy.objects.SBox;
import samy.objects.SFaceIndexed;
import samy.objects.SFacesIndexed;
import samy.objects.SInteger;
import samy.external.SJava;
import samy.objects.SStyle;
import samy.objects3D.SFaces3D;
import samy.objects3D.SLines3D;
import samy.objects3D.SVertex3D;
import samy.objects3D.SFacesIndexed3D;
import samy.objects3D.SObject3D;
import samy.objects3D.SObjects3D;
import samy.objects3D.SPoint3D;
import samy.objects3D.SShape3D;
import samy.objects3D.SShapes3D;

public class SAsc {

    private SObjects3D objects3d;
    public SInteger progress;

    /**
     * Constructor
     */
    public SAsc(String path) {
        this.loadAsc(path, true, true, SObjects3D.angleLimit);
    }

    /**
     * Constructor
     */
    public SAsc(String path, boolean enableVertexsNormals, boolean enableEdges) {
        this.loadAsc(path, enableVertexsNormals, enableEdges, SObjects3D.angleLimit);
    }

    /**
     * Constructor
     */
    public SAsc(String path, boolean enableVertexsNormals, boolean enableEdges, double angleLimit, SInteger progress) {
        this.progress = progress;
        this.loadAsc(path, enableVertexsNormals, enableEdges, angleLimit);
    }

    /**
     * Constructor de copia
     */
    public SAsc(SAsc asc) {
        this.objects3d = asc.objects3d.getCopy();
    }

    /**
     * Obtener una copia del objeto
     */
    public SAsc getCopy() {
        return new SAsc(this);
    }

    /**
     * Función de carga para geometrias de tipo *.asc
     */
    private void loadAsc(String path, boolean enableVertexsNormals, boolean enableEdges, double angleLimit) {
        String pathInLowercase = path.toLowerCase();
        if (pathInLowercase.contains(".asc")) {
            loadAscFile(path, enableVertexsNormals, enableEdges, angleLimit);
        }
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
     * Función especifica para la carga de ficheros *.asc
     */
    protected boolean loadAscFile(String path, boolean enableVertexsNormals, boolean enableEdges, double angleLimit) {
        setProgressValue(10);
        String[] lines = SJava.loadStrings(path);
        setProgressValue(20);
        if (lines != null) {
            processAscFile(lines, enableVertexsNormals, enableEdges, angleLimit);
            return true;
        } else {
            System.out.println("file" + path + "not found");
            return false;
        }
    }

    /**
     * Función especifica para procesar ficheros *.asc
     */
    protected void processAscFile(String[] lines, boolean enableVertexsNormals, boolean enableEdges, double angleLimit) {
        this.objects3d = new SObjects3D();
        int lineIndex = 0;
        while (true) {
            if (lineIndex > lines.length - 1) {
                break;
            }
            String line = lines[lineIndex];

            //Buscamos: Named object:
            while (!line.contains("Named object:")) {
                lineIndex++;
                if (lineIndex > lines.length) {
                    break;
                }
                line = lines[lineIndex];
            }

            if (lineIndex > lines.length) {
                break;
            }

            lineIndex++;//Named object: 
            line = lines[lineIndex++];//Tri-mesh, Vertices: 8     Faces: 12            
            int indexNvertexs = line.indexOf("Vertices: "); //Leemos el numero de vertices
            int indexNfaces = line.indexOf("Faces: "); //Leemos el numero de caras  
            int nvertexs = Integer.parseInt(line.substring(indexNvertexs + 10, indexNfaces - 5));
            int nfaces = Integer.parseInt(line.substring(indexNfaces + 7));

            //2. Cargamos los vertices
            SVertex3D[] vertexsArrray = new SVertex3D[nvertexs];
            lineIndex++;//Vertex list:                          
            for (int i = 0; i < nvertexs; i++) {
                line = lines[lineIndex];
                lineIndex++;
                int indexX = line.indexOf("X:");
                int indexY = line.indexOf("Y:");
                int indexZ = line.indexOf("Z:");
                String strX = line.substring(indexX + 2, indexY - 1);
                String strY = line.substring(indexY + 2, indexZ - 1);
                String strZ = line.substring(indexZ + 2);
                float x = (float) SNumeric.eval(strX);
                float y = (float) SNumeric.eval(strY);
                float z = (float) SNumeric.eval(strZ);
                SVertex3D vertex = new SVertex3D(x, y, z);
                vertexsArrray[i] = vertex;
                setProgressValue((int)(20 + 20*(float)((float)i/(float)nvertexs)));
            }

            //3. Cargamos las caras indexadas
            SFacesIndexed facesIndexed = new SFacesIndexed();
            lineIndex++;//Face list:    
            for (int i = 0; i < nfaces; i++) {
                SFaceIndexed faceIndexed = new SFaceIndexed();

                //Recogemos los indices
                line = lines[lineIndex];
                lineIndex++;
                int indexV1 = line.indexOf("A:");
                int indexV2 = line.indexOf("B:");
                int indexV3 = line.indexOf("C:");
                int indexV4 = line.indexOf("AB:");
                faceIndexed.add(Integer.parseInt(line.substring(indexV1 + 2, indexV2 - 1)));
                faceIndexed.add(Integer.parseInt(line.substring(indexV2 + 2, indexV3 - 1)));
                faceIndexed.add(Integer.parseInt(line.substring(indexV3 + 2, indexV4 - 1)));

                //Recogemos los sombreados de las caras            
                line = lines[lineIndex];
                lineIndex++;
                String myLine = line.substring(10);
                int indexR = myLine.indexOf("r");
                int indexG = myLine.indexOf("g");
                int indexB = myLine.indexOf("b");
                int indexA = myLine.indexOf("a");
                int indexE = myLine.indexOf("\"");

                int r = Integer.parseInt(myLine.substring(indexR + 1, indexG)); //r 
                int g = Integer.parseInt(myLine.substring(indexG + 1, indexB)); //g
                int b = Integer.parseInt(myLine.substring(indexB + 1, indexA)); //b
                int a = 255 - Integer.parseInt(myLine.substring(indexA + 1, indexE)); //a
                faceIndexed.styleFill = new SStyle(r, g, b, a);

                facesIndexed.add(faceIndexed);

                lineIndex++;
                setProgressValue((int)(40 + 30*(float)((float)i/(float)nvertexs)));
            }

            //4. Construimos un grupo de caras indexadas
            SFacesIndexed3D facesIndexed3D = new SFacesIndexed3D(vertexsArrray, facesIndexed);
            setProgressValue(70);

            //5. Calculamos las normales y aristas
            SLines3D edges = facesIndexed3D.computeNormalsAndEdges(enableEdges, enableVertexsNormals, angleLimit);
            edges.setStyle(new SStyle(0, 0, 0, 255, 2));
            setProgressValue(80);

            //6. Calculamos las caras
            SFaces3D faces = facesIndexed3D.getFaces3D();
            setProgressValue(90);

            //7. Construimos el objeto3D
            SObject3D object3d = new SObject3D(faces, edges);
            objects3d.add(object3d);
            setProgressValue(100);
        }
    }

    /**
     * Trasladar
     */
    public void translate(SPoint3D p) {
        this.objects3d.translate(p.x, p.y, p.z);
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
     * Aplicar el espejo en X
     */
    public void mirrorX(double x) {
        this.objects3d.mirrorX(x);
    }

    /**
     * Aplicar el espejo en Y
     */
    public void mirrorY(double y) {
        this.objects3d.mirrorY(y);
    }

    /**
     * Aplicar el espejo en Z
     */
    public void mirrorZ(double z) {
        this.objects3d.mirrorZ(z);
    }

    /**
     * Obtener el espejo en X
     */
    public SAsc getMirrorX(double x) {
        SAsc object = this.getCopy();
        object.mirrorX(x);
        return object;
    }

    /**
     * Obtener el espejo en Y
     */
    public SAsc getMirrorY(double y) {
        SAsc object = this.getCopy();
        object.mirrorY(y);
        return object;
    }

    /**
     * Obtener el espejo en Z
     */
    public SAsc getMirrorZ(double z) {
        SAsc object = this.getCopy();
        object.mirrorZ(z);
        return object;
    }

    /**
     * Obtener la traslacion del objeto
     */
    public SAsc getTranslation(double x, double y, double z) {
        SAsc object = this.getCopy();
        object.translate(x, y, z);
        return object;
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
        getEdges().setStyle(style);
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
