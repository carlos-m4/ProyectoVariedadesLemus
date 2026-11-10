package model;

public class Producto {

    private int idProducto;
    private String nombre;
    private String descripcion;
    private String categoria;
    private double precio;
    private String estado;

    private int idInventario;
    private double precioCosto;     // Lo que costó comprarlo
    private int stock;              // Cuántos hay
    private int stockMinimo;        // Si hay menos que esto, avisamos

    public Producto() {
        this.estado = "Activo";
    }

    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    double precio, String estado, int idInventario, double precioCosto,
                    int stock, int stockMinimo) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.precio = precio;
        this.estado = estado;
        this.idInventario = idInventario;
        this.precioCosto = precioCosto;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getIdInventario() { return idInventario; }
    public void setIdInventario(int idInventario) { this.idInventario = idInventario; }

    public double getPrecioCosto() { return precioCosto; }
    public void setPrecioCosto(double precioCosto) { this.precioCosto = precioCosto; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public boolean isStockBajo() {
        return stock <= stockMinimo;
    }


    public boolean estaActivo() {
        return estado == null || estado.equalsIgnoreCase("Activo");
    }

    @Override
    public String toString() {
        return nombre;
    }
}