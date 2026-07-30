import app.core.QrCode;
import java.io.*;
public class Dump {
    public static void main(String[] a) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) continue;
            String[] p = line.split("\t", -1);
            QrCode q = QrCode.encode(p[0], QrCode.Ecc.valueOf(p[1]), Integer.parseInt(p[2]));
            out.println("###\t" + q.version + "\t" + q.mode + "\t" + q.mask + "\t" + q.size);
            for (int y = 0; y < q.size; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < q.size; x++) sb.append(q.module(x, y) ? '1' : '0');
                out.println(sb);
            }
        }
    }
}
