import axios from 'axios';
import { Cookies } from 'react-cookie';

export class AxiosHelper {
    public static initializeAxios() {
        const cookies = new Cookies();
        const xsrfToken = cookies.get("XSRF-TOKEN");
        if (xsrfToken) {
            axios.interceptors.request.use(request => {
                request.headers["XSRF-TOKEN"] = xsrfToken;
                return request;
            });
        }
    }
}
