package controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GeneradorPDF {
    private static final double ANCHO = 595;
    private static final double ALTO = 842;
    private static final double MARGEN = 45;
    private static final double ALTO_FILA = 18;
    private static final int FILAS_POR_PAGINA = 34;
    private static final Charset WINDOWS = Charset.forName("windows-1252");

    private GeneradorPDF() {
    }

    public static void crear(File archivo, String titulo, String subtitulo,
                             String[] encabezados, double[] proporciones, List<String[]> filas) throws IOException {
        List<List<String[]>> paginas = new ArrayList<>();
        for (int i = 0; i < filas.size(); i += FILAS_POR_PAGINA) {
            paginas.add(filas.subList(i, Math.min(i + FILAS_POR_PAGINA, filas.size())));
        }
        if (paginas.isEmpty()) {
            paginas.add(new ArrayList<>());
        }

        double[] anchos = calcularAnchos(proporciones);
        String generado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        List<byte[]> contenidos = new ArrayList<>();
        for (int i = 0; i < paginas.size(); i++) {
            contenidos.add(dibujarPagina(titulo, subtitulo, encabezados, anchos, paginas.get(i),
                    i + 1, paginas.size(), generado));
        }
        escribirArchivo(archivo, contenidos);
    }

    private static double[] calcularAnchos(double[] proporciones) {
        double suma = 0;
        for (double p : proporciones) {
            suma += p;
        }
        double disponible = ANCHO - 2 * MARGEN;
        double[] anchos = new double[proporciones.length];
        for (int i = 0; i < proporciones.length; i++) {
            anchos[i] = disponible * proporciones[i] / suma;
        }
        return anchos;
    }

    private static byte[] dibujarPagina(String titulo, String subtitulo, String[] encabezados, double[] anchos,
                                        List<String[]> filas, int pagina, int totalPaginas, String generado) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        double y = ALTO - MARGEN;

        color(out, 0.055, 0.204, 0.361);
        texto(out, "F2", 16, MARGEN, y, "Variedades Lemus");
        color(out, 0.1, 0.1, 0.1);
        texto(out, "F2", 13, MARGEN, y - 22, titulo);
        color(out, 0.39, 0.45, 0.55);
        texto(out, "F1", 9, MARGEN, y - 38, subtitulo);

        double yTabla = y - 62;
        double anchoTotal = ANCHO - 2 * MARGEN;

        comando(out, String.format(Locale.US, "0.055 0.204 0.361 rg %.2f %.2f %.2f %.2f re f\n",
                MARGEN, yTabla - 5, anchoTotal, ALTO_FILA));
        color(out, 1, 1, 1);
        double x = MARGEN;
        for (int c = 0; c < encabezados.length; c++) {
            texto(out, "F2", 9, x + 4, yTabla, recortar(encabezados[c], anchos[c], 9));
            x += anchos[c];
        }

        double yFila = yTabla - ALTO_FILA;
        for (int f = 0; f < filas.size(); f++) {
            String[] fila = filas.get(f);
            boolean esTotal = fila.length > 0 && fila[0] != null && fila[0].startsWith("TOTAL");

            if (f % 2 == 1 || esTotal) {
                double gris = esTotal ? 0.88 : 0.96;
                comando(out, String.format(Locale.US, "%.2f %.2f %.2f rg %.2f %.2f %.2f %.2f re f\n",
                        gris, gris, gris, MARGEN, yFila - 5, anchoTotal, ALTO_FILA));
            }

            color(out, 0.1, 0.1, 0.1);
            x = MARGEN;
            for (int c = 0; c < anchos.length; c++) {
                String valor = c < fila.length && fila[c] != null ? fila[c] : "";
                texto(out, esTotal ? "F2" : "F1", 9, x + 4, yFila, recortar(valor, anchos[c], 9));
                x += anchos[c];
            }
            yFila -= ALTO_FILA;
        }

        comando(out, String.format(Locale.US, "0.8 0.8 0.8 RG 0.5 w %.2f %.2f m %.2f %.2f l S\n",
                MARGEN, yFila + ALTO_FILA - 5, MARGEN + anchoTotal, yFila + ALTO_FILA - 5));

        if (filas.isEmpty()) {
            color(out, 0.39, 0.45, 0.55);
            texto(out, "F1", 10, MARGEN, yFila, "No hay datos para este reporte.");
        }

        color(out, 0.39, 0.45, 0.55);
        texto(out, "F1", 8, MARGEN, 28, "Generado el " + generado);
        String numero = "Página " + pagina + " de " + totalPaginas;
        texto(out, "F1", 8, ANCHO - MARGEN - numero.length() * 8 * 0.5, 28, numero);

        return out.toByteArray();
    }

    private static String recortar(String valor, double ancho, double tamano) {
        int maximo = (int) ((ancho - 8) / (tamano * 0.5));
        if (valor.length() <= maximo) {
            return valor;
        }
        return valor.substring(0, Math.max(0, maximo - 3)) + "...";
    }

    private static void color(ByteArrayOutputStream out, double r, double g, double b) {
        comando(out, String.format(Locale.US, "%.3f %.3f %.3f rg\n", r, g, b));
    }

    private static void texto(ByteArrayOutputStream out, String fuente, double tamano,
                              double x, double y, String valor) {
        comando(out, String.format(Locale.US, "BT /%s %.1f Tf %.2f %.2f Td (", fuente, tamano, x, y));
        out.writeBytes(escapar(valor));
        comando(out, ") Tj ET\n");
    }

    private static byte[] escapar(String valor) {
        CharsetEncoder codificador = WINDOWS.newEncoder();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (char c : valor.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') {
                bytes.write('\\');
                bytes.write(c);
            } else if (codificador.canEncode(c)) {
                bytes.writeBytes(String.valueOf(c).getBytes(WINDOWS));
            } else {
                bytes.write('?');
            }
        }
        return bytes.toByteArray();
    }

    private static void comando(ByteArrayOutputStream out, String texto) {
        out.writeBytes(texto.getBytes(StandardCharsets.US_ASCII));
    }

    private static void escribirArchivo(File archivo, List<byte[]> contenidos) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> posiciones = new ArrayList<>();
        int totalObjetos = 4 + contenidos.size() * 2;

        escribir(pdf, "%PDF-1.4\n");
        pdf.writeBytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        posiciones.add(pdf.size());
        escribir(pdf, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < contenidos.size(); i++) {
            hijos.append(5 + i * 2).append(" 0 R ");
        }
        posiciones.add(pdf.size());
        escribir(pdf, "2 0 obj\n<< /Type /Pages /Kids [" + hijos + "] /Count " + contenidos.size() + " >>\nendobj\n");

        posiciones.add(pdf.size());
        escribir(pdf, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

        posiciones.add(pdf.size());
        escribir(pdf, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int i = 0; i < contenidos.size(); i++) {
            int numPagina = 5 + i * 2;
            int numContenido = numPagina + 1;

            posiciones.add(pdf.size());
            escribir(pdf, numPagina + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + numContenido + " 0 R >>\nendobj\n");

            byte[] contenido = contenidos.get(i);
            posiciones.add(pdf.size());
            escribir(pdf, numContenido + " 0 obj\n<< /Length " + contenido.length + " >>\nstream\n");
            pdf.writeBytes(contenido);
            escribir(pdf, "\nendstream\nendobj\n");
        }

        int inicioTabla = pdf.size();
        escribir(pdf, "xref\n0 " + (totalObjetos + 1) + "\n0000000000 65535 f \n");
        for (int posicion : posiciones) {
            escribir(pdf, String.format("%010d 00000 n \n", posicion));
        }
        escribir(pdf, "trailer\n<< /Size " + (totalObjetos + 1) + " /Root 1 0 R >>\nstartxref\n"
                + inicioTabla + "\n%%EOF\n");

        try (OutputStream salida = new FileOutputStream(archivo)) {
            pdf.writeTo(salida);
        }
    }

    private static void escribir(ByteArrayOutputStream out, String texto) {
        out.writeBytes(texto.getBytes(StandardCharsets.US_ASCII));
    }
}