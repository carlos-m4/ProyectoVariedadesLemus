package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class Venta {

    public static final double IVA = 0.13;


    private int idVenta;
    private LocalDate fecha;
    private double total;
    private String cliente;
    private int idUsuario;


    private String nombreUsuario;
    private String metodoPago;
    private double efectivoRecibido;
    private double cambio;


    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
        this.fecha = LocalDate.now();
        this.cliente = "Cliente final";
        this.metodoPago = "Efectivo";
    }

    public Venta(int idVenta, LocalDate fecha, double total, String cliente, int idUsuario) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.total = total;
        this.cliente = cliente;
        this.idUsuario = idUsuario;
        this.metodoPago = "Efectivo";
    }

    public double getSubtotal() {
        double suma = 0;
        for (DetalleVenta d : detalles) {
            suma += d.getTotal();
        }
        return redondear(suma);
    }

    public double getMontoIva() {
        return redondear(getSubtotal() * IVA);
    }

    public double calcularTotal() {
        this.total = redondear(getSubtotal() + getMontoIva());
        return total;
    }

    public int getCantidadProductos() {
        int cuenta = 0;
        for (DetalleVenta d : detalles) {
            cuenta += d.getCantidad();
        }
        return cuenta;
    }

    public static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public double getEfectivoRecibido() { return efectivoRecibido; }
    public void setEfectivoRecibido(double efectivoRecibido) { this.efectivoRecibido = efectivoRecibido; }

    public double getCambio() { return cambio; }
    public void setCambio(double cambio) { this.cambio = cambio; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
}