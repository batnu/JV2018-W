/** 
 * Proyecto: Juego de la vida.
 *  Resuelve todos los aspectos del almacenamiento del DTO Patron utilizando un ArrayList.
 *  Colabora en el patron Fachada.
 *  @since: prototipo2.0
 *  @source: SesionesDAO.java 
 *  @version: 2.0 - 2019/03/23 
 *  @author: ajp
 */

package accesoDatos.fichero;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import accesoDatos.DatosException;
import accesoDatos.OperacionesDAO;
import config.Configuracion;
import modelo.ModeloException;
import modelo.SesionUsuario;
import modelo.Usuario;

public class SesionesDAO implements OperacionesDAO, Persistente {

	// Singleton.
	private static SesionesDAO instancia = null;
	private static File fSesiones;

	// Elemento de almacenamiento. 
	private ArrayList<SesionUsuario> datosSesiones;

	/**
	 * Constructor por defecto de uso interno.
	 * Sólo se ejecutará una vez.
	 */
	private SesionesDAO() {
		datosSesiones = new ArrayList<SesionUsuario>();
		fSesiones = new File(Configuracion.get().getProperty("sesiones.nombreFichero"));
		try {
			recuperarDatos();
		} 
		catch (DatosException e) { 
		}
	}

	/**
	 *  Método estático de acceso a la instancia única.
	 *  Si no existe la crea invocando al constructor interno.
	 *  Utiliza inicialización diferida.
	 *  Sólo se crea una vez; instancia única -patrón singleton-
	 *  @return instancia
	 */
	public static SesionesDAO getInstancia() {
		if (instancia == null) {
			instancia = new SesionesDAO();
		}
		return instancia;
	}

	// OPERACIONES DE PERSISTENCIA
	/**
	 *  Recupera el Arraylist sesionesUsuario almacenados en fichero. 
	 * @throws DatosException 
	 */
	@Override
	public void recuperarDatos() throws DatosException {
		try {
			if (fSesiones.exists()) {
				FileInputStream fisSesiones = new FileInputStream(fSesiones);
				ObjectInputStream oisSesiones = new ObjectInputStream(fisSesiones);
				datosSesiones = (ArrayList<SesionUsuario>) oisSesiones.readObject();
				oisSesiones.close();
				return;
			}
			throw new DatosException("El fichero de datos: " + fSesiones.getName() + " no existe...");
		} 
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Guarda el Arraylist de sesiones de usuarios en fichero.
	 */
	@Override
	public void guardarDatos() {
		guardarDatos(datosSesiones);
	}

	/**
	 *  Guarda la lista recibida en el fichero de datos.
	 */
	private void guardarDatos(List<SesionUsuario> listaSesiones) {
		try {
			FileOutputStream fosSesiones = new FileOutputStream(fSesiones);
			ObjectOutputStream oosSesiones = new ObjectOutputStream(fosSesiones);
			oosSesiones.writeObject(datosSesiones);		
			oosSesiones.flush();
			oosSesiones.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 *  Cierra almacenes de datos.
	 */
	@Override
	public void cerrar() {
		guardarDatos();
	}
	
	//OPERACIONES DAO
	/**
	 * Búsqueda de sesión por idSesion.
	 * @param id - el idUsr+fecha a buscar.
	 * @return - la sesión encontrada; sin encuentra. 
	 */
	@Override
	public SesionUsuario obtener(String id) {
		assert id != null;
		int posicion = indexSort(id);					// En base 1
		if (posicion >= 0) {
			return datosSesiones.get(posicion - 1);     // En base 0
		}
		return null;
	}
	
	/**
	 *  Obtiene por búsqueda binaria, la posición que ocupa, o ocuparía,  una sesión en 
	 *  la estructura.
	 *	@param id - id de Sesion a buscar.
	 *	@return - la posición, en base 1, que ocupa un objeto o la que ocuparía (negativo).
	 */
	private int indexSort(String id) {
		int comparacion;
		int inicio = 0;
		int fin = datosSesiones.size() - 1;
		int medio = 0;
		while (inicio <= fin) {
			medio = (inicio + fin) / 2;			// Calcula posición central.
			// Obtiene > 0 si idSesion va después que medio.
			comparacion = id.compareTo(datosSesiones.get(medio).getId());
			if (comparacion == 0) {			
				return medio + 1;   			// Posción ocupada, base 1	  
			}		
			if (comparacion > 0) {
				inicio = medio + 1;
			}			
			else {
				fin = medio - 1;
			}
		}	
		return -(inicio + 1);					// Posición que ocuparía -negativo- base 1
	}

	/**
	 * obtiene todas las sesiones en una lista.
	 * @return - la lista.
	 */
	@Override
	public List obtenerTodos() {
		return datosSesiones;
	}

	/**
	 * Búsqueda de todas la sesiones de un mismo usuario.
	 * @param idUsr - el identificador de usuario a buscar.
	 * @return - Sublista con las sesiones encontrada.
	 * @throws ModeloException 
	 * @throws DatosException - si no existe ninguna.
	 */
	public List<SesionUsuario> obtenerTodasMismoUsr(String idUsr) throws ModeloException, DatosException  {
		assert idUsr != null;
		SesionUsuario aux = new SesionUsuario();
		aux.setUsr(UsuariosDAO.getInstancia().obtener(idUsr));
		//Busca posición inserción ordenada por idUsr + fecha. La última para el mismo usuario.
		return separarSesionesUsr(indexSort(aux.getId()) - 1);
	}

	/**
	 * Separa en una lista independiente de todas las sesiones de un mismo usuario.
	 * @param ultima - el indice de una sesion almacenada.
	 * @return - Sublista con las sesiones encontrada; null si no existe ninguna.
	 */
	private List<SesionUsuario> separarSesionesUsr(int ultima) {
		String idUsr = datosSesiones.get(ultima).getUsr().getId();
		int primera = ultima;
		// Localiza primera sesión del mismo usuario.
		for (int i = ultima; i >= 0 && datosSesiones.get(i).getUsr().getId().equals(idUsr); i--) {
			primera = i;
		}
		// devuelve la sublista de sesiones buscadas.
		return datosSesiones.subList(primera, ultima+1);
	}

	/**
	 * Alta de una nueva SesionUsuario en orden y sin repeticiones según IdUsr + fecha. 
	 * Busca previamente la posición que le corresponde por búsqueda binaria.
	 * @param obj - la SesionUsuario a almacenar.
	 * @throws DatosException - si ya existe.
	 */
	@Override
	public void alta(Object obj) throws DatosException  {
		assert obj != null;
		SesionUsuario sesionNueva = (SesionUsuario) obj;							// Para conversión cast
		int posInsercion = indexSort(sesionNueva.getId()); 
		if (posInsercion < 0) {
			datosSesiones.add(Math.abs(posInsercion)-1, sesionNueva); 				// Inserta la sesión en orden.
		}
		else {
			throw new DatosException("SesionesDAO.alta: "+ sesionNueva.getId() + " ya existe");
		}
	}

	/**
	 * Elimina el objeto, dado el id utilizado para el almacenamiento.
	 * @param idSesion - identificador de la SesionUsuario a eliminar.
	 * @return - la SesionUsuario eliminada.
	 * @throws DatosException - si no existe.
	 */
	@Override
	public SesionUsuario baja(String idSesion) throws DatosException  {
		assert idSesion != null;
		int posicion = indexSort(idSesion); 										// En base 1
		if (posicion > 0) {
			return datosSesiones.remove(posicion - 1); 								// En base 0
		}
		else {
			throw new DatosException("SesionesDAO.baja: "+ idSesion + " no existe");
		}
	}
	
	/**
	 *  Actualiza datos de una SesionUsuario reemplazando el almacenado por el recibido.
	 *	@param obj - SesionUsuario con las modificaciones.
	 * @throws DatosException - si no existe.
	 */
	@Override
	public void actualizar(Object obj) throws DatosException {
		assert obj != null;
		SesionUsuario sesionActualizada = (SesionUsuario) obj;				// Para conversión cast
		int posicion = indexSort(sesionActualizada.getId()); 				// En base 1
		if (posicion > 0) {
			// Reemplaza elemento
			datosSesiones.set(posicion - 1, sesionActualizada);  			// En base 0		
		}
		else {
			throw new DatosException("SesionesDAO.actualizar: "+ sesionActualizada.getId() + " no existe");
		}
	}

	/**
	 * Obtiene el listado de todos las sesiones almacenadas.
	 * @return el texto con el volcado de datos.
	 */
	@Override
	public String listarDatos() {
		StringBuilder result = new StringBuilder();
		for (SesionUsuario sesiones: datosSesiones) {
			result.append("\n" + sesiones);
		}
		return result.toString();
	}

	/**
	 * Obtiene el listado de todos id de los objetos almacenados.
	 * @return el texto con el volcado de id.
	 */
	@Override
	public String listarId() {
		StringBuilder result = new StringBuilder();
		for (SesionUsuario sesiones: datosSesiones) {
			result.append("\n" + sesiones.getId()); 
		}
		return result.toString();
	}
	
	/**
	 * Elimina todos las sesiones almacenadas.
	 */
	@Override
	public void borrarTodo() {
		datosSesiones.clear();	
	}

	/**
	 * Total de sesiones almacenadas.
	 */
	public int totalRegistrado() {
		return datosSesiones.size();
	}

}//class
