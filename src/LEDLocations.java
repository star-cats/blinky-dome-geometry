/**
 * Created by akesich on 5/28/17.
 */
import java.io.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class LEDLocations {
    public static BufferedWriter outWriter;
    private static final double EDGE_LENGTH = 23.0;

    public static void main(String[] args) {
        String csvFile = "vertex-locations.csv";
        BufferedReader br = null;
        String line;
        String cvsSplitBy = ",";

        try {
            File outFile = new File("led-locations.csv");
            if (!outFile.exists()) {
                outFile.createNewFile();
            }

            FileWriter fw = new FileWriter(outFile.getAbsoluteFile());
            outWriter = new BufferedWriter(fw);

            outWriter.write("index,subindex,x,y,z\n");

            br = new BufferedReader(new FileReader(csvFile));
            br.readLine(); // throw away header line
            while ((line = br.readLine()) != null) {
                String[] vars = line.split(cvsSplitBy);

                Vector3D vertex1 = new Vector3D(
                    Double.parseDouble(vars[2]),
                    Double.parseDouble(vars[3]),
                    Double.parseDouble(vars[4])
                );

                Vector3D vertex2 = new Vector3D(
                    Double.parseDouble(vars[5]),
                    Double.parseDouble(vars[6]),
                    Double.parseDouble(vars[7])
                );

                Vector3D vertex3 = new Vector3D(
                    Double.parseDouble(vars[8]),
                    Double.parseDouble(vars[9]),
                    Double.parseDouble(vars[10])
                );

                Vector3D center = vertex1.add(vertex2).add(vertex3).scalarMultiply(1.0/3);

                Vector3D offset1 = vertex1.subtract(center).normalize().scalarMultiply(EDGE_LENGTH/1.732);
                Vector3D offset2 = vertex2.subtract(center).normalize().scalarMultiply(EDGE_LENGTH/1.732);
                Vector3D offset3 = vertex3.subtract(center).normalize().scalarMultiply(EDGE_LENGTH/1.732);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
