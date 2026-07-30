import app.core.*;
import app.core.export.*;
import java.io.File;
public class Export {
    public static void main(String[] a) throws Exception {
        String code = a.length > 0 ? a[0] : "TST-0000-00-001";
        LabelModel m = LabelModel.defaults();
        m.setSigla("F04");
        SerialWindow w = SerialWindow.of(code, 3);
        String[] codes = w.run(3);
        PngExporter.write(m, codes[0], new File("/tmp/exp/etichetta.png"));
        SvgExporter.write(m, codes[0], new File("/tmp/exp/etichetta.svg"));
        PdfExporter.write(m, codes, new File("/tmp/exp/giro.pdf"));
        System.out.println("esportati: " + codes[0] + " .. " + codes[codes.length-1]);
        for (String warn : m.warnings(codes[0], 0.30)) System.out.println("  attenzione: " + warn);
    }
}
