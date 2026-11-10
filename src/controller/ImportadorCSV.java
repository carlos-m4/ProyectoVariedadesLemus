package controller;

import model.Producto;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ImportadorCSV {

    public static class Resultado {
        public final List<Producto> productos = new ArrayList<>();
        public final List<String> errores = new ArrayList<>();
    }

    private ImportadorCSV() {
    }

    public static Resultado leer(File archivo) throws IOException {
        Resultado resultado = new Resultado();
        List<String> lineas = leerLineas(archivo.toPath());

        if (lineas.isEmpty()) {
            resultado.errores.add("El archivo está vacío.");
            return resultado;
        }

        String primera = quitarMarcaInvisible(lineas.get(0));
        lineas.set(0, primera);
        char separador = primera.contains(";") ? ';' : ',';

        int inicio = primera.toLowerCase().contains("nombre") ? 1 : 0;

        for (int i = inicio; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            int numeroLinea = i + 1;

            if (linea.isBlank()) {
                continue;
            }

            List<String> columnas = partir(linea, separador);
            if (columnas.size() < 7) {
                resultado.errores.add("Línea " + numeroLinea + ": tiene " + columnas.size()
                        + " columnas y se necesitan 7.");
                continue;
            }

            try {
                Producto p = new Producto();
                p.setNombre(columnas.get(0).trim());
                p.setDescripcion(columnas.get(1).trim());
                p.setCategoria(columnas.get(2).trim());
                p.setPrecioCosto(SubPanelINventarioController.leerPrecio(columnas.get(3), "precio de compra"));
                p.setPrecio(SubPanelINventarioController.leerPrecio(columnas.get(4), "precio de venta"));
                p.setStock(SubPanelINventarioController.leerEntero(columnas.get(5), "stock"));
                p.setStockMinimo(SubPanelINventarioController.leerEntero(columnas.get(6), "stock mínimo"));

                String problema = SubPanelINventarioController.validar(p);
                if (problema != null) {
                    resultado.errores.add("Línea " + numeroLinea + ": " + problema);
                } else {
                    resultado.productos.add(p);
                }
            } catch (NumberFormatException e) {
                resultado.errores.add("Línea " + numeroLinea + ": " + e.getMessage());
            }
        }
        return resultado;
    }

    private static List<String> leerLineas(Path ruta) throws IOException {
        try {
            return new ArrayList<>(Files.readAllLines(ruta, StandardCharsets.UTF_8));
        } catch (MalformedInputException e) {
            return new ArrayList<>(Files.readAllLines(ruta, Charset.forName("windows-1252")));
        }
    }

    private static String quitarMarcaInvisible(String texto) {
        return texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
    }


    private static List<String> partir(String linea, char separador) {
        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean dentroDeComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);

            if (c == '"') {
                if (dentroDeComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    dentroDeComillas = !dentroDeComillas;
                }
            } else if (c == separador && !dentroDeComillas) {
                partes.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        partes.add(actual.toString());
        return partes;
    }
}