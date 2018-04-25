# 3DImporters
3D importers for several file formats


## Introducción

El objetivo de este documento es describir las nuevas capacidades de interoperatividad de nuestro software de impresión 3D.

Es normal que los usuarios de un sistema de impresión 3d utilicen mas de una herramienta en las diferentes fases de su proceso creativo.

- elaboración de modelos
- preprocesado para impresión3d
- impresión.

### Elaboración de modelos

La elaboración de modelos se lleva a cabo con paquetes libres o comerciales de CAD: Autocad, Freecad, SolidWorks, etc. Los usuarios profesionales no suelen querer cambiar la herramienta suelen con la que trabajan debido al elevado coste de las licencias y al tiempo que se necesita para dominarlas.

### Preprocesado

Estas herramientas trabajan con los modelos obtenidos con las herramientas de modelado y los convierten a formatos de fichero adecuados al proceso de impresión.

En la fase de preprocesado se utilizan los modelos Las herramientas de preprocesado sirven para preparar los modelos 3D para ser imprimidos. Con estas herramientas de realizan verificaciones para determinar si el modelo es apto para ser imprimido, se añaden características especificas como materiales y soportes.

Hemos decidido que para mejorar la interoperatividad la estrategia correcta es integrarnos con herramientas de preprocesado a nivel de formato de fichero de salida.

### Impresión

Nuestro software está diseñado para instalarse en la propia impresora y cubrir las necesidades de la fase final de impresión. Al estar orientado al hardware de nuestra impresora nos permite ajustar y optimizar todas las fases del proceso.

## Selección de Herramientas para la integración

Hemos hecho un trabajo de investigación para seleccionar paquetes de software de preprocesado para impresión3D con los que integrarnos, siguiendo estos criterios:

- Que aporten funcionalmente al proceso de creación de modelos para los futuros usuarios.
- Que sea software de fabricantes de primera linea mundial, con cierta garantía de calidad y continuidad.
- Que tengan versión libre o al menos gratuita.
- Que se prevea cierto nivel de adopción por parte de la comunidad.

Los paquetes seleccionados son:

- Microsoft 3d Builder
- Autodesk Meshmixer

## Microsoft 3D builder

 Es un programa orientado a usuarios con poca experiencia en la impresión 3D. Aunque le faltan muchas funcionalidades es precisamente
su sencillez lo que le hace see interesante para este tipo de usuarios.
Microsoft 3D builder viene instalado de serie en Windows 10 por lo que está inmediatamente disponible para cualquier usuario.

El formato de fichero para la impresión 3d de Microsoft 3d Builder es el 3MF. Este formato es relativamente nuevo (2015) y por tanto no ha alcanzado la madurez y niveles de adopción de otros formatos mas antiguos. Permite definir materiales, impresoras y el resto de parámetros relativos a la impresión. Es de esperar que se produzcan cambios y nuevas versiones de este standar.

Ejemplo de uso con samyStudio


# Autodesk meshmixer

Es un programa de preprocesado para impresión3d ofrecido de forma gratuita por Autodesk. Al contrario del Microsoft3D builder es 
una herramienta llena de funcionalidades aunque compleja para ser usada por usuarios sin experiencia. Sus funcionalidades incluyen el calculo de soportes, calculo de piezas huecas y con rellenos
y la asignación de materiales.

El formato de intercambio de MeshMixer es el STL o "Standar Tessellation Language". Es un formato mucho más antiguo que 3mf y que no permite el uso de materiales de forma directa. Sin embargo sigue siendo uno de los formatos preferidos para la impresión3d.  STL tiene dos sub-formatos ASCII y Binario. La ventaja de este ultimo es que es mucho más compacto y fácil de procesar. Por compatibilidad implementaremos ambos formatos de STL.

Ejemplos de uso con samyStudio.

## formatos de impresión seleccionados

Finalmente los formatos de impresión seleccionados para su implementación son: 3mf y stl. 
El código fuente de estos desarrollos se encuentra en 
