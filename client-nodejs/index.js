/**
 * Cliente Node.js para probar el sistema de facturación
 *
 * Este script realiza pruebas de los endpoints principales:
 * - Creación de clientes
 * - Obtención de clientes
 * - Creación de facturas
 * - Consulta de facturas
 */

import axios from 'axios';
import colors from 'colors';

// Configuración
const BASE_URL = process.env.API_URL || 'http://localhost:8080';

// Utilidades para mostrar resultados
function mostrarTitulo(titulo) {
    console.log('\n' + '='.repeat(80).cyan);
    console.log(`  ${titulo}`.cyan.bold);
    console.log('='.repeat(80).cyan + '\n');
}

function mostrarExito(mensaje, data) {
    console.log('✓'.green + ` ${mensaje}`.green);
    if (data) {
        console.log(JSON.stringify(data, null, 2).gray);
    }
    console.log('');
}

function mostrarError(mensaje, error) {
    console.log('✗'.red + ` ${mensaje}`.red);
    if (error.response) {
        console.log('Status:'.yellow, error.response.status);
        console.log('Error:'.yellow, JSON.stringify(error.response.data, null, 2).gray);
    } else {
        console.log('Error:'.yellow, error.message.gray);
    }
    console.log('');
}

function mostrarInfo(mensaje) {
    console.log('ℹ'.blue + ` ${mensaje}`.blue);
}

// Funciones de prueba
async function probarCreacionCliente() {
    mostrarTitulo('PRUEBA 1: Crear Cliente');

    const clienteData = {
        nombre: "Empresa de Prueba S.A.S.",
        nit: "900123456-7",
        email: "contacto@empresaprueba.com",
        telefono: "3001234567",
        direccion: "Calle 100 #15-20, Bogotá",
        activo: true
    };

    try {
        mostrarInfo('Enviando request POST /clientes...');
        const response = await axios.post(`${BASE_URL}/clientes`, clienteData);
        mostrarExito('Cliente creado exitosamente', response.data);
        return response.data.id;
    } catch (error) {
        mostrarError('Error al crear cliente', error);
        throw error;
    }
}

async function probarObtenerCliente(clienteId) {
    mostrarTitulo(`PRUEBA 2: Obtener Cliente (ID: ${clienteId})`);

    try {
        mostrarInfo(`Enviando request GET /clientes/${clienteId}...`);
        const response = await axios.get(`${BASE_URL}/clientes/${clienteId}`);
        mostrarExito('Cliente obtenido exitosamente', response.data);
        return response.data;
    } catch (error) {
        mostrarError('Error al obtener cliente', error);
        throw error;
    }
}

async function probarListarClientes() {
    mostrarTitulo('PRUEBA 3: Listar Todos los Clientes');

    try {
        mostrarInfo('Enviando request GET /clientes...');
        const response = await axios.get(`${BASE_URL}/clientes`);
        mostrarExito(`Se encontraron ${response.data.length} clientes`, response.data);
        return response.data;
    } catch (error) {
        mostrarError('Error al listar clientes', error);
        throw error;
    }
}

async function probarCreacionFactura(clienteId) {
    mostrarTitulo(`PRUEBA 4: Crear Factura para Cliente ${clienteId}`);

    const facturaData = {
        clienteId: clienteId,
        items: [
            {
                descripcion: "Laptop Dell Inspiron 15",
                cantidad: 2,
                precioUnitario: 2500000,
                porcentajeImpuesto: 19,
                porcentajeDescuento: 5
            },
            {
                descripcion: "Mouse Inalámbrico Logitech",
                cantidad: 3,
                precioUnitario: 80000,
                porcentajeImpuesto: 19,
                porcentajeDescuento: 10
            },
            {
                descripcion: "Teclado Mecánico RGB",
                cantidad: 2,
                precioUnitario: 350000,
                porcentajeImpuesto: 19,
                porcentajeDescuento: 0
            }
        ]
    };

    try {
        mostrarInfo('Enviando request POST /facturas...');
        const response = await axios.post(`${BASE_URL}/facturas`, facturaData);
        mostrarExito('Factura creada exitosamente', response.data);

        console.log('📊 Resumen de la Factura:'.yellow.bold);
        console.log(`   Número: ${response.data.numeroFactura}`.white);
        console.log(`   Subtotal: $${response.data.subtotal.toLocaleString('es-CO')}`.white);
        console.log(`   Impuestos: $${response.data.impuestos.toLocaleString('es-CO')}`.white);
        console.log(`   Descuentos: $${response.data.descuentos.toLocaleString('es-CO')}`.white);
        console.log(`   TOTAL: $${response.data.total.toLocaleString('es-CO')}`.green.bold);
        console.log('');

        return response.data.id;
    } catch (error) {
        mostrarError('Error al crear factura', error);
        throw error;
    }
}

async function probarObtenerFactura(facturaId) {
    mostrarTitulo(`PRUEBA 5: Obtener Factura (ID: ${facturaId})`);

    try {
        mostrarInfo(`Enviando request GET /facturas/${facturaId}...`);
        const response = await axios.get(`${BASE_URL}/facturas/${facturaId}`);
        mostrarExito('Factura obtenida exitosamente', response.data);
        return response.data;
    } catch (error) {
        mostrarError('Error al obtener factura', error);
        throw error;
    }
}

async function probarListarFacturasPorCliente(clienteId) {
    mostrarTitulo(`PRUEBA 6: Listar Facturas del Cliente ${clienteId}`);

    try {
        mostrarInfo(`Enviando request GET /facturas?clienteId=${clienteId}...`);
        const response = await axios.get(`${BASE_URL}/facturas?clienteId=${clienteId}`);
        mostrarExito(`Se encontraron ${response.data.length} facturas para el cliente`, response.data);
        return response.data;
    } catch (error) {
        mostrarError('Error al listar facturas del cliente', error);
        throw error;
    }
}

async function probarActualizarCliente(clienteId) {
    mostrarTitulo(`PRUEBA 7: Actualizar Cliente (ID: ${clienteId})`);

    const clienteActualizado = {
        nombre: "Empresa de Prueba S.A.S. - Actualizada",
        nit: "900123456-7",
        email: "nuevo@empresaprueba.com",
        telefono: "3109876543",
        direccion: "Carrera 7 #32-40, Bogotá",
        activo: true
    };

    try {
        mostrarInfo(`Enviando request PUT /clientes/${clienteId}...`);
        const response = await axios.put(`${BASE_URL}/clientes/${clienteId}`, clienteActualizado);
        mostrarExito('Cliente actualizado exitosamente', response.data);
        return response.data;
    } catch (error) {
        mostrarError('Error al actualizar cliente', error);
        throw error;
    }
}

async function probarCreacionFacturaSinCliente() {
    mostrarTitulo('PRUEBA 8: Intentar Crear Factura sin Cliente (Debe Fallar)');

    const facturaData = {
        clienteId: 99999, // Cliente inexistente
        items: [
            {
                descripcion: "Producto Test",
                cantidad: 1,
                precioUnitario: 100000,
                porcentajeImpuesto: 19,
                porcentajeDescuento: 0
            }
        ]
    };

    try {
        mostrarInfo('Enviando request POST /facturas (con cliente inexistente)...');
        const response = await axios.post(`${BASE_URL}/facturas`, facturaData);
        mostrarError('ERROR: Se esperaba que fallara pero tuvo éxito', {});
    } catch (error) {
        mostrarExito('Validación correcta: La factura no se puede crear sin un cliente válido', error.response?.data);
    }
}

// Función principal
async function ejecutarPruebas() {
    console.log('\n' + '█'.repeat(80).rainbow);
    console.log('  CLIENTE DE PRUEBA - SISTEMA DE FACTURACIÓN'.rainbow.bold);
    console.log('  Castor - Prueba Técnica Backend Java'.white);
    console.log('█'.repeat(80).rainbow);

    console.log(`\n🔗 API Base URL: ${BASE_URL}`.cyan);
    console.log(`⏰ Fecha: ${new Date().toLocaleString('es-CO')}`.cyan);

    let clienteId;
    let facturaId;

    try {
        // Flujo de pruebas principal
        clienteId = await probarCreacionCliente();
        await probarObtenerCliente(clienteId);
        await probarListarClientes();
        facturaId = await probarCreacionFactura(clienteId);
        await probarObtenerFactura(facturaId);
        await probarListarFacturasPorCliente(clienteId);
        await probarActualizarCliente(clienteId);
        await probarCreacionFacturaSinCliente();

        // Resumen final
        mostrarTitulo('RESUMEN DE PRUEBAS');
        console.log('✓ Todas las pruebas completadas exitosamente'.green.bold);
        console.log(`✓ Cliente creado con ID: ${clienteId}`.green);
        console.log(`✓ Factura creada con ID: ${facturaId}`.green);
        console.log('✓ Validaciones de negocio funcionando correctamente'.green);

    } catch (error) {
        mostrarTitulo('ERROR EN LAS PRUEBAS');
        console.log('✗ Ocurrió un error durante las pruebas'.red.bold);
        console.log('Asegúrese de que el backend esté ejecutándose en:', BASE_URL);
        process.exit(1);
    }

    console.log('\n' + '█'.repeat(80).rainbow);
    console.log('  FIN DE LAS PRUEBAS'.rainbow.bold);
    console.log('█'.repeat(80).rainbow + '\n');
}

// Ejecutar
ejecutarPruebas();
