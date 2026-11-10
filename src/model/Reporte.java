package model;

import java.time.LocalDate;


public class Reporte {

    private int idReporte;
    private String cliente;
    private String tipo;
    private String descripcion;
    private double monto;
    private String estado;
    private LocalDate fecha;
    private Integer idVenta;


    public Reporte() {
        this.fecha = LocalDate.now();
        this.estado = "Pendiente";
        this.monto = 0.0;
    }

    public Reporte(int idReporte, String cliente, String tipo, String descripcion,
                   double monto, String estado, LocalDate fecha, Integer idVenta) {
        this.idReporte = idReporte;
        this.cliente = cliente;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.monto = monto;
        this.estado = estado;
        this.fecha = fecha;
        this.idVenta = idVenta;
    }

    public int getIdReporte() { return idReporte; }
    public void setIdReporte(int idReporte) { this.idReporte = idReporte; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Integer getIdVenta() { return idVenta; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }

    public boolean estaPendiente() {
        return estado == null || estado.equalsIgnoreCase("Pendiente");
    }
}