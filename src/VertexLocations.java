/**
 * Created by akesich on 5/20/17.
 */
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import javax.xml.bind.ValidationException;

import static java.lang.Math.*;

public class VertexLocations {
    private static BufferedWriter vertexOutWriter;
    private static BufferedWriter ledOutWriter;
    private static BufferedWriter starLedOutWriter;
    private static final int ITERATION = 4;
    private static final double RADIUS = 187.121;

    private static final double EDGE_LENGTH = 23.0;
    private static final double LED_SPACING = 100.0/2.54/60.0;
    private static final int LEDS_PER_SIDE = 35; // I know this is a hack; fuck you

    public static void main(String[] args) {
        try {
            File vertexOutFile = new File("vertex-locations.csv");
            File ledOutFile = new File("led-locations.csv");
            File starOutFile = new File("star-led-locations.csv");
            if (!vertexOutFile.exists()) {
                vertexOutFile.createNewFile();
            }

            if (!ledOutFile.exists()) {
                vertexOutFile.createNewFile();
            }

            FileWriter vfw = new FileWriter(vertexOutFile.getAbsoluteFile());
            vertexOutWriter = new BufferedWriter(vfw);

            FileWriter lfw = new FileWriter(ledOutFile.getAbsoluteFile());
            ledOutWriter = new BufferedWriter(lfw);

            vertexOutWriter.write("index,sub_index");
            vertexOutWriter.write(",vertex_1_x,vertex_1_y,vertex_1_z");
            vertexOutWriter.write(",vertex_2_x,vertex_2_y,vertex_2_z");
            vertexOutWriter.write(",vertex_3_x,vertex_3_y,vertex_3_z");
            vertexOutWriter.write("\n");

            ledOutWriter.write("index,sub_index,led_num," +
                               "points_up,layer," +
                               "x,y,z," +
                               "triangle_center_x,triangle_center_y,triangle_center_z," +
                               "pp_group,pp_strip,pp_index,\n");

            double phi_high = PI/2 - atan(0.5);
            double phi_low = PI - phi_high;

            for (int i = 0; i < 5; i++) {
                Vector3D vertex_1 = Vector3D.PLUS_K.scalarMultiply(RADIUS);
                Vector3D vertex_2 = sphericalVector(RADIUS, i*2*PI/5, phi_high);
                Vector3D vertex_3 = sphericalVector(RADIUS, (i+1)*2*PI/5, phi_high);

                subdivideTriangle(i, vertex_1, vertex_2, vertex_3);
            }

            for (int i = 0; i < 5; i++) {
                Vector3D vertex_1 = sphericalVector(RADIUS, i * 2 * PI / 5, phi_high);
                Vector3D vertex_2 = sphericalVector(RADIUS, (2 * i + 1) * PI / 5, phi_low);
                Vector3D vertex_3 = sphericalVector(RADIUS, (2 * i - 1) * PI / 5, phi_low);

                subdivideTriangle(i + 5, vertex_1, vertex_2, vertex_3);
            }

            for (int i = 0; i < 5; i++) {
                Vector3D vertex_1 = sphericalVector(RADIUS, (2 * i + 1) * PI / 5, phi_low);
                Vector3D vertex_2 = sphericalVector(RADIUS, (2 * i + 2) * PI / 5, phi_high);
                Vector3D vertex_3 = sphericalVector(RADIUS, (2 * i) * PI / 5, phi_high);

                subdivideTriangle(i + 10, vertex_1, vertex_2, vertex_3);
            }

            for (int i = 0; i < 5; i++) {
                Vector3D vertex_1 = Vector3D.MINUS_K.scalarMultiply(RADIUS);
                Vector3D vertex_2 = sphericalVector(RADIUS, (2 * i - 1) * PI / 5, phi_low);
                Vector3D vertex_3 = sphericalVector(RADIUS, (2 * i + 1) * PI / 5, phi_low);

                subdivideTriangle(i + 15, vertex_1, vertex_2, vertex_3);
            }

            vertexOutWriter.close();
            ledOutWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static Vector3D sphericalVector(double r, double theta, double phi) {
        return new Vector3D(r*sin(phi)*cos(theta), r*sin(phi)*sin(theta), r*cos(phi));
    }

    private static void writeTriangle(int index, int subIndex, Vector3D A, Vector3D B, Vector3D C) {
        try {
            System.out.println(A.distance(B));
            vertexOutWriter.write(String.format("%d,%d", index, subIndex));
            writeVector(vertexOutWriter, A);
            writeVector(vertexOutWriter, B);
            writeVector(vertexOutWriter, C);
            vertexOutWriter.write("\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeVector(BufferedWriter writer, Vector3D vector) {
        try {
            writer.write(String.format(",%g,%g,%g", vector.getX(), vector.getY(), vector.getZ()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void subdivideTriangle(int index, Vector3D vertexA, Vector3D vertexB, Vector3D vertexC) {
        Vector3D A = vertexA.scalarMultiply(1.0/ITERATION);
        Vector3D B = vertexB.scalarMultiply(1.0/ITERATION);
        Vector3D C = vertexC.scalarMultiply(1.0/ITERATION);
        int sub_index = 0;
        for (int a = 0; a < ITERATION; a++) {
            for (int b = 0; b < ITERATION - a; b++) {
                int c = ITERATION - a - b;
                Vector3D vertex1  = A.scalarMultiply(a).add(B.scalarMultiply(b)).add(C.scalarMultiply(c));
                Vector3D vertex2 = vertex1.add(A).subtract(C);
                Vector3D vertex3 = vertex1.add(B).subtract(C);

                vertex1 = vertex1.normalize().scalarMultiply(RADIUS);
                vertex2 = vertex2.normalize().scalarMultiply(RADIUS);
                vertex3 = vertex3.normalize().scalarMultiply(RADIUS);

                if (vertex1.getZ() > -0.01 && vertex2.getZ() > -0.01 && vertex3.getZ() > - 0.01) {
                    writeTriangle(index, sub_index, vertex1, vertex2, vertex3);
                    findLEDs(index, sub_index, vertex1, vertex2, vertex3);
                }

                sub_index++;

                if (c > 1) {
                    Vector3D vertex4  = A.scalarMultiply(a + 1).add(B.scalarMultiply(b + 1)).add(C.scalarMultiply(c - 2));
                    vertex4 = vertex4.normalize().scalarMultiply(RADIUS);
                    if (vertex4.getZ() > -0.01 && vertex2.getZ() > -0.01 && vertex3.getZ() > - 0.01) {
                        writeTriangle(index, sub_index, vertex4, vertex2, vertex3);
                        findLEDs(index, sub_index, vertex4, vertex2, vertex3);
                    }

                    sub_index++;
                }
            }
        }
    }

    private static void findLEDs(int index, int subIndex, Vector3D A, Vector3D B, Vector3D C) {
        if (!isInPattern(index, subIndex)) {
            return;
        }
        boolean up = pointsUp(index, subIndex);
        Vector3D center = A.add(B).add(C).scalarMultiply(1.0/3);

        Vector3D primaryAxis;

        if (up) {
            if (A.getZ() < B.getZ() && A.getZ() < C.getZ()) {
                primaryAxis = A;
            } else if (B.getZ() < C.getZ()) {
                primaryAxis = B;
            } else {
                primaryAxis = C;
            }
        } else {
            if (A.getZ() > B.getZ() && A.getZ() > C.getZ()) {
                primaryAxis = A;
            } else if (B.getZ() > C.getZ()) {
                primaryAxis = B;
            } else {
                primaryAxis = C;
            }
        }

        Vector3D offset1 = primaryAxis.subtract(center).normalize().scalarMultiply(-EDGE_LENGTH/1.732);
        Vector3D offset2 = rotateVector(offset1, center, 2 * PI / 3);
        Vector3D offset3 = rotateVector(offset1, center, 4 * PI / 3);

        Vector3D vertex1 = center.add(offset1);
        Vector3D vertex2 = center.add(offset2);
        Vector3D vertex3 = center.add(offset3);

        int num_leds = writeLEDStrip(index, subIndex, vertex1, vertex2, 0, up, center);
        num_leds = writeLEDStrip(index, subIndex, vertex2, vertex3, num_leds, up, center);
        num_leds = writeLEDStrip(index, subIndex, vertex3, vertex1, num_leds, up, center);

    }

    private static boolean isInPattern(int index, int subIndex) {
        if (index < 5 && subIndex == 15) {
            return true;
        }
        return subIndex == 13
                || subIndex == 9
                || subIndex == 5
                || subIndex == 1;
    }

    private static int getLayer(int index, int subIndex) {
        if (index < 5) {
            if (subIndex == 15) {
                return 0;
            } else if (subIndex >= 12) {
                return 1;
            } else if (subIndex >= 7) {
                return 2;
            } else {
                return 3;
            }
        }
        if (index < 10) {
            if (subIndex == 15) {
                return 4;
            } else if (subIndex >= 12) {
                return 5;
            } else if (subIndex >= 7) {
                return 6;
            } else {
                return 7;
            }
        }
        if (index < 15) {
            if (subIndex == 15) {
                return 7;
            } else if (subIndex >= 12) {
                return 6;
            } else if (subIndex >= 7) {
                return 5;
            } else {
                return 4;
            }
        }
        return -1;
    }

    private static boolean matchZ(Vector3D A, Vector3D B) {
        return Math.abs(A.getZ() - B.getZ()) < 0.1;
    }

    private static boolean pointsUp(int index, int subIndex) {
        boolean topUp =
                subIndex == 2
                        || subIndex == 4
                        || subIndex == 6
                        || subIndex == 9
                        || subIndex == 11
                        || subIndex == 14;
        if (index < 10) {
            return topUp;
        } else {
            return !topUp;
        }
    }

    private static int writeLEDStrip(int index, int subIndex, Vector3D start, Vector3D end, int offset, boolean up, Vector3D triangleCenter) {
        double distance = 0;
        double strip_length = start.distance(end);
//        System.out.println(strip_length);
        int strip_count = 0;

        Vector3D norm = end.subtract(start).normalize();

        while (distance < strip_length && strip_count < LEDS_PER_SIDE) { // what did I say about fucking off?
            writeLED(
                    index,
                    subIndex,
                    offset,
                    start.add(norm.scalarMultiply(distance)),
                    up,
                    triangleCenter,
                    getPPGroup(index, subIndex),
                    getPPStrip(index, subIndex),
                    getPPIndex(index, subIndex, offset)
            );
            distance += LED_SPACING;
            offset++;
            strip_count++;
        }

        return offset;
    }

    private static void writeLED(
            int index,
            int subIndex,
            int ledNumber,
            Vector3D position,
            boolean up,
            Vector3D triangleCenter,
            int ppGroup,
            int ppStrip,
            int ppIndex
    ) {
        int layer = getLayer(index, subIndex);
        try {
            ledOutWriter.write(String.format("%d,%d,%d,%d,%d", index, subIndex, ledNumber, up ? 1 : 0, layer));
            writeVector(ledOutWriter, position);
            writeVector(ledOutWriter, triangleCenter);
            ledOutWriter.write(String.format(",%d,%d,%d",ppGroup,ppStrip,ppIndex));
            ledOutWriter.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getPPGroup (int triangleIndex, int triangleSubindex) {
        if (triangleIndex < 2) {
            if (triangleSubindex >= 13) {
                return 0;
            }
        }
        return -1;
    }

    private static int getPPStrip (int triangleIndex, int triangleSubindex) {
        if (triangleIndex < 2) {
            if (triangleSubindex >= 13) {
                return 1;
            }
        }
        return -1;
    }

    private static int getPPIndex (int triangleIndex, int triangleSubindex, int offset) {
        switch (triangleIndex) {
            case 0:
                if (triangleSubindex == 15) {
                    return offset;
                } else if (triangleSubindex == 13){
                    return offset + 105;
                }
                return -1;
            case 1:
                if (triangleSubindex == 15) {
                    return offset + 105 * 2;
                } else if (triangleSubindex == 13){
                    return offset + 105 * 3;
                }
                return -1;
            default:
                return -1;
        }
    }

    // https://en.wikipedia.org/wiki/Rotation_matrix#Rotation_matrix_from_axis_and_angle
    private static Vector3D rotateVector(Vector3D vec, Vector3D axis, double th) {
        axis = axis.normalize();
        double ux = axis.getX();
        double uy = axis.getY();
        double uz = axis.getZ();

        double vx = vec.getX();
        double vy = vec.getY();
        double vz = vec.getZ();

        double x = (cos(th) + ux * ux * (1 - cos(th))) * vx
                + (ux * uy * (1 - cos(th)) - uz * sin(th)) * vy
                + (ux * uz * (1 - cos(th)) + uy * sin(th)) * vz;

        double y = (ux * uy * (1 - cos(th)) + uz * sin(th)) * vx
                + (cos(th) + uy * uy * (1 - cos(th))) * vy
                + (uy * uz * (1 - cos(th)) - ux * sin(th)) * vz;

        double z = (ux * uz * (1 - cos(th)) - uy * sin(th)) * vx
                + (uy * uz * (1 - cos(th)) + ux * sin(th)) * vy
                + (cos(th) + uz * uz * (1 - cos(th))) * vz;

        return new Vector3D(x, y, z);
    }
}
