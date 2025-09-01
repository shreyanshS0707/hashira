import java.util.*;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class app {

    public static void main(String[] args) {
        // Test Case 1
        try {
            Map<String, Object> testCase1 = loadTestCaseFromFile("test1.json");
            BigInteger result1 = solveSecretSharing(testCase1);
            // print only c for test1
            System.out.println(result1.toString());
        } catch (Exception e) {
            System.err.println("Error loading test1.json: " + e.getMessage());
        }

        // Test Case 2
        try {
            Map<String, Object> testCase2 = loadTestCaseFromFile("test2.json");
            BigInteger result2 = solveSecretSharing(testCase2);
            // print only c for test2
            System.out.println(result2.toString());
        } catch (Exception e) {
            System.err.println("Error loading test2.json: " + e.getMessage());
        }
    }

    /**
     * Load test case data from JSON file
     */
    public static Map<String, Object> loadTestCaseFromFile(String filename) throws IOException {
        // Read JSON file content
        String jsonContent = new String(Files.readAllBytes(Paths.get(filename)));

        // Parse JSON using Gson
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> data = gson.fromJson(jsonContent, type);

        return data;
    }

    /**
     * Main function to solve the secret sharing problem
     */
    public static BigInteger solveSecretSharing(Map<String, Object> data) {
        // Extract keys - handle both Integer and Double from JSON parsing
        Map<String, Object> keys = (Map<String, Object>) data.get("keys");
        int n = getIntValue(keys.get("n"));
        int k = getIntValue(keys.get("k"));

        // Parse and convert all points
        List<Point> points = parsePoints(data, n);

        // Select points for interpolation with validation
        List<Point> selectedPoints = selectPointsForInterpolation(points, k);

        // Calculate constant term using Lagrange interpolation
        BigInteger constantTerm = calculateConstantTerm(selectedPoints);

        return constantTerm;
    }

    /**
     * Helper method to convert Object to int (handles Double from JSON parsing)
     */
    private static int getIntValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    /**
     * Parse points from JSON-like structure and convert bases to decimal
     */
    public static List<Point> parsePoints(Map<String, Object> data, int n) {
        List<Point> points = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            String key = String.valueOf(i);
            if (data.containsKey(key)) {
                Map<String, Object> pointData = (Map<String, Object>) data.get(key);
                int base = getIntValue(pointData.get("base"));
                String value = pointData.get("value").toString();

                try {
                    // Convert from given base to decimal using BigInteger
                    BigInteger decimalValue = convertToDecimal(value, base);
                    points.add(new Point(i, decimalValue));
                } catch (Exception e) {
                    // conversion errors go to stderr so they don't mix with the c output
                    System.err.println("Error converting point " + i + ": " + e.getMessage());
                }
            }
        }

        return points;
    }

    /**
     * Convert a number from any base to decimal using BigInteger
     */
    public static BigInteger convertToDecimal(String value, int base) {
        return new BigInteger(value, base);
    }

    /**
     * Calculate the constant term using Lagrange interpolation with BigInteger arithmetic
     * The constant term is the value of the polynomial at x = 0
     */
    public static BigInteger calculateConstantTerm(List<Point> points) {
        BigDecimal result = BigDecimal.ZERO;

        for (int i = 0; i < points.size(); i++) {
            Point pi = points.get(i);
            BigDecimal term = new BigDecimal(pi.y);

            // Calculate Lagrange basis polynomial Li(0)
            BigDecimal numerator = BigDecimal.ONE;
            BigDecimal denominator = BigDecimal.ONE;

            for (int j = 0; j < points.size(); j++) {
                if (i != j) {
                    Point pj = points.get(j);
                    // Li(0) = Π((0 - xj)/(xi - xj)) for j ≠ i
                    numerator = numerator.multiply(BigDecimal.valueOf(-pj.x));
                    denominator = denominator.multiply(BigDecimal.valueOf(pi.x - pj.x));
                }
            }

            // Calculate Li(0)
            BigDecimal lagrangeBasis = numerator.divide(denominator, 50, RoundingMode.HALF_UP);
            term = term.multiply(lagrangeBasis);
            result = result.add(term);
        }

        return result.setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }

    /**
     * Select k points for interpolation with validation
     */
    public static List<Point> selectPointsForInterpolation(List<Point> allPoints, int k) {
        if (allPoints.size() < k) {
            throw new IllegalStateException(
                    String.format("Insufficient points for reconstruction. Need %d, got %d", k, allPoints.size())
            );
        }

        // Take first k points (any k points work for Lagrange interpolation)
        List<Point> selected = allPoints.subList(0, k);

        // Validate no duplicate x-coordinates
        Set<Integer> xCoords = new HashSet<>();
        for (Point p : selected) {
            if (!xCoords.add(p.x)) {
                throw new IllegalStateException("Duplicate x-coordinate found: " + p.x);
            }
        }

        return selected;
    }
    static class Point {
        int x;
        BigInteger y;

        Point(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }
}
