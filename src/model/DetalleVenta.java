package model;

public class DetalleVenta {

    private int idDetalle;
    private int idVenta;
    private int idProducto;
    private String nombreProducto;
    private double precioUnitario;
    private int cantidad;
    private double total;


    public DetalleVenta() {
    }


    public DetalleVenta(int idProducto, String nombreProducto, double precioUnitario, int cantidad) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        calcularTotal();
    }

    public DetalleVenta(int idDetalle, int idVenta, int idProducto, String nombreProducto,
                        int cantidad, double total) {
        this.idDetalle = idDetalle;
        this.idVenta = idVenta;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.total = total;
        this.precioUnitario = cantidad > 0 ? total / cantidad : 0;
    }


    public void calcularTotal() {
        this.total = Math.round(precioUnitario * cantidad * 100.0) / 100.0;
    }


    public void sumarUno() {
        cantidad++;
        calcularTotal();
    }

    public int getIdDetalle() { return idDetalle; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
        calcularTotal();
    }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        calcularTotal();
    }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}