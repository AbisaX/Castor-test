package com.castor.facturacion.domain.port.out;

import com.castor.facturacion.domain.ItemFactura;

import java.util.List;

/**
 * Puerto de Salida (Driven Port)
 * Define el contrato para calcular impuestos y descuentos.
 *
 * Este puerto se implementa en la infraestructura para comunicarse
 * con el microservicio de Python (tax-calculator-service).
 */
public interface TaxCalculatorPort {

    /**
     * Calcular impuestos y descuentos para una lista de items
     *
     * @param items Lista de items de factura
     * @return Lista de items con impuestos y descuentos calculados
     */
    List<ItemFactura> calcularImpuestosYDescuentos(List<ItemFactura> items);
}
